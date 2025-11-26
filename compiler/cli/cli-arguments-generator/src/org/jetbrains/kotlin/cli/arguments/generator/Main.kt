/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.arguments.generator

import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.arguments.dsl.types.StringArrayType
import org.jetbrains.kotlin.cli.common.arguments.Disables
import org.jetbrains.kotlin.cli.common.arguments.Enables
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

private val COPYRIGHT by lazy { File("license/COPYRIGHT_HEADER.txt").readText() }
private const val ORIGIN_FILE_PATH = "compiler/arguments/src/org/jetbrains/kotlin/arguments/description"

fun main(args: Array<String>) {
    val genDir = File(args[0])
    for (level in args.drop(1)) {
        generateLevel(genDir, level)
    }
}

private fun generateLevel(genDir: File, levelName: String) {
    val (level, parent) = findLevelWithParent(levelName)
    generateArgumentsClass(genDir, level, parent)
}

private fun findLevelWithParent(name: String): Pair<KotlinCompilerArgumentsLevel, KotlinCompilerArgumentsLevel?> {
    fun find(level: KotlinCompilerArgumentsLevel, parent: KotlinCompilerArgumentsLevel?): Pair<KotlinCompilerArgumentsLevel, KotlinCompilerArgumentsLevel?>? {
        if (level.name == name) return level to parent
        return level.nestedLevels.firstNotNullOfOrNull { find(it, level) }
    }
    return find(kotlinCompilerArguments.topLevel, null) ?: error("Level with name $name not found")
}

class ArgumentsInfo(
    val levelName: String,
    val className: String,
    val classPackage: String = "org.jetbrains.kotlin.cli.common.arguments.",
    val configuratorName: String? = "${className}Configurator",
    val levelIsFinal: Boolean,
    val originFileName: String = className,
    val additionalSyntheticArguments: List<String> = emptyList(),
    val additionalGenerator: SmartPrinter.() -> Unit = {},
)

val ArgumentsInfo.isCommonToolsArgs: Boolean
    get() = levelName == CompilerArgumentsLevelNames.commonToolArguments

val ArgumentsInfo.isCommonCompilerArgs: Boolean
    get() = levelName == CompilerArgumentsLevelNames.commonCompilerArguments

val levelToClassNameMap = listOf(
    ArgumentsInfo(
        levelName = CompilerArgumentsLevelNames.commonToolArguments,
        className = "CommonToolArguments",
        configuratorName = null,
        levelIsFinal = false,
        additionalGenerator = SmartPrinter::generateFreeArgsAndErrors,
    ),
    ArgumentsInfo(
        levelName = CompilerArgumentsLevelNames.commonCompilerArguments,
        className = "CommonCompilerArguments",
        levelIsFinal = false,
        additionalSyntheticArguments = listOf("autoAdvanceLanguageVersion", "autoAdvanceApiVersion"),
        additionalGenerator = SmartPrinter::generateDummyImpl,
    ),
    ArgumentsInfo(
        levelName = CompilerArgumentsLevelNames.jvmCompilerArguments,
        className = "K2JVMCompilerArguments",
        levelIsFinal = true,
        originFileName = "JvmCompilerArguments",
    ),
    ArgumentsInfo(
        levelName = CompilerArgumentsLevelNames.commonKlibBasedArguments,
        className = "CommonKlibBasedCompilerArguments",
        levelIsFinal = false,
    ),
    ArgumentsInfo(
        levelName = CompilerArgumentsLevelNames.wasmArguments,
        className = "K2WasmCompilerArguments",
        levelIsFinal = false,
        originFileName = "WasmCompilerArguments",
    ),
    ArgumentsInfo(
        levelName = CompilerArgumentsLevelNames.jsArguments,
        className = "K2JSCompilerArguments",
        levelIsFinal = true,
        originFileName = "JsCompilerArguments",
    ),
    ArgumentsInfo(
        levelName = CompilerArgumentsLevelNames.nativeArguments,
        className = "K2NativeCompilerArguments",
        levelIsFinal = true,
        originFileName = "NativeCompilerArguments",
    ),
    ArgumentsInfo(
        levelName = CompilerArgumentsLevelNames.nativeKlibArguments,
        className = "K2NativeKlibCompilerArguments",
        levelIsFinal = true,
        originFileName = "NativeKlibCompilerArguments",
    ),
    ArgumentsInfo(
        levelName = CompilerArgumentsLevelNames.metadataArguments,
        className = "K2MetadataCompilerArguments",
        levelIsFinal = true,
        originFileName = "MetadataCompilerArguments",
    ),
).associateBy { it.levelName }

// Removed arguments which are still needed in CLI classes but should be hidden
private val hiddenArguments = setOf(
    CompilerArgumentsLevelNames.jsArguments to "output", // Needed by IDEA
    CompilerArgumentsLevelNames.commonCompilerArguments to "Xuse-k2", // Needed by IDEA
)

private fun generateArgumentsClass(
    genDir: File,
    level: KotlinCompilerArgumentsLevel,
    parent: KotlinCompilerArgumentsLevel?,
) {
    val info = levelToClassNameMap.getValue(level.name)
    val packagePath = info.classPackage
        .dropLastWhile { it == '.' }
        .split(".")
    var dir = genDir
    for (packagePart in packagePath) {
        dir = dir.resolve(packagePart)
    }
    dir.mkdirs()
    val file = dir.resolve(info.className + ".kt")
    val newText = buildString { SmartPrinter(this).generateArgumentsClass(level, parent, info) }
    GeneratorsFileUtil.writeFileIfContentChanged(file, newText, logNotChanged = false)
}

private fun SmartPrinter.generateArgumentsClass(
    level: KotlinCompilerArgumentsLevel,
    parent: KotlinCompilerArgumentsLevel?,
    info: ArgumentsInfo
) {
    println(COPYRIGHT)
    println("package org.jetbrains.kotlin.cli.common.arguments")
    println()

    val imports = level.collectImports(info)
    if (imports.isNotEmpty()) {
        imports.forEach { println(it) }
        println()
    }

    print(GeneratorsFileUtil.GENERATED_MESSAGE_PREFIX)
    println("generator in :compiler:cli:cli-arguments-generator")
    println("// Please declare arguments in $ORIGIN_FILE_PATH/${info.originFileName}.kt")
    println(GeneratorsFileUtil.GENERATED_MESSAGE_SUFFIX)
    println()

    if (!info.levelIsFinal) {
        print("abstract ")
    }
    print("class ${info.className}")
    val supertypes = when (parent) {
        null -> "Freezable(), Serializable"
        else -> "${levelToClassNameMap.getValue(parent.name).className}()"
    }
    println(" : $supertypes {")
    withIndent {
        generateAdditionalSyntheticArguments(info)
        for (argument in level.arguments) {
            if (
                hiddenArguments.none { (argLevelName, name) ->
                    argLevelName == level.name && argument.name == name
                } && argument.releaseVersionsMetadata.removedVersion != null
            ) continue
            validateDeprecationConsistency(argument)
            generateGradleAnnotations(argument)
            generateArgumentAnnotation(argument, level)
            generateFeatureAnnotations(argument)
            generateProperty(argument)
            println()
        }
        generateConfigurator(info)
        generateCopyOf(info)
        info.additionalGenerator.invoke(this)
    }
    println("}")
}

private fun KotlinCompilerArgumentsLevel.collectImports(info: ArgumentsInfo): List<String> {
    val rawImports = buildSet {
        add("org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArgumentsConfigurator")
        if (info.levelIsFinal || info.isCommonCompilerArgs) {
            add("com.intellij.util.xmlb.annotations.Transient")
        }
        if (info.isCommonToolsArgs) {
            add("java.io.Serializable")
        }
        arguments.flatMapTo(this) { argument ->
            argument.additionalAnnotations.flatMap {
                when (it) {
                    is Enables -> listOf(Enables::class.qualifiedName!!, LanguageFeature::class.qualifiedName!!)
                    is Disables -> listOf(Disables::class.qualifiedName!!, LanguageFeature::class.qualifiedName!!)
                    is Deprecated -> emptyList()
                    else -> error("Unknown annotation ${it::class}")
                }

            }
        }
    }
    return rawImports
        .sorted()
        .filter { it.dropLastWhile { it != '.' } != info.classPackage }
        .map { "import $it" }
}

private fun SmartPrinter.generateAdditionalSyntheticArguments(info: ArgumentsInfo) {
    for (argument in info.additionalSyntheticArguments) {
        println("@get:Transient")
        println("var $argument: Boolean = true")
        generateSetter(type = "Boolean", argument = null)
        println()
    }
}

private fun SmartPrinter.generateArgumentAnnotation(
    argument: KotlinCompilerArgument,
    level: KotlinCompilerArgumentsLevel,
) {
    println("@Argument(")
    withIndent {
        println("""value = "-${argument.name}",""")
        argument.shortName?.let { println("""shortName = "-$it",""") }
        argument.deprecatedName?.let { println("""deprecatedName = "-$it",""") }
        argument.valueDescription.current?.let { println("""valueDescription = "$it",""") }
        val rawDescription = argument.description.current.replace("\"", """\"""")
        val description = if ("\n" in rawDescription) {
            "$tripleQuote$rawDescription$tripleQuote"
        } else {
            "\"$rawDescription\""
        }
        println("description = $description,")
        argument.delimiter?.let { println("delimiter = Argument.Delimiters.${it.constantName},") }

        if (hiddenArguments.any { (levelName, argName) ->
                level.name == levelName && argument.name == argName
            }
        ) {
            println("isObsolete = true,")
        }
    }
    println(")")
}

private enum class AnnotationKind {
    Gradle,
    LanguageFeature
}

private fun validateDeprecationConsistency(argument: KotlinCompilerArgument) {
    if (argument.releaseVersionsMetadata.removedVersion != null) return
    val deprecatedAnnotation = argument.additionalAnnotations.firstIsInstanceOrNull<Deprecated>()
    val deprecatedVersion = argument.releaseVersionsMetadata.deprecatedVersion
    when {
        deprecatedVersion == null && deprecatedAnnotation != null -> {
            error("Argument ${argument.name} is deprecated but has no deprecated version specified")
        }
        deprecatedVersion != null && deprecatedAnnotation == null -> {
            error("Argument ${argument.name} is deprecated but has no @Deprecated annotation")
        }
    }
}

private fun SmartPrinter.generateGradleAnnotations(argument: KotlinCompilerArgument) {
    generateAdditionalAnnotations(argument, kind = AnnotationKind.Gradle)
}

private fun SmartPrinter.generateFeatureAnnotations(argument: KotlinCompilerArgument) {
    generateAdditionalAnnotations(argument, kind = AnnotationKind.LanguageFeature)
}

private fun SmartPrinter.generateAdditionalAnnotations(argument: KotlinCompilerArgument, kind: AnnotationKind) {
    for (annotation in argument.additionalAnnotations) {
        generateAnnotation(annotation, kind)
    }
}

private fun SmartPrinter.generateAnnotation(annotation: Annotation, kind: AnnotationKind) {
    when (annotation) {
        is Enables if kind == AnnotationKind.LanguageFeature -> {
            val feature = annotation.feature
            val ifValue = annotation.ifValueIs
            val featureName = feature.name
            val optionalValue = if (ifValue.isNotBlank()) ", \"$ifValue\"" else ""
            println("@Enables(LanguageFeature.$featureName$optionalValue)")
        }
        is Disables if kind == AnnotationKind.LanguageFeature-> {
            val feature = annotation.feature
            val ifValue = annotation.ifValueIs
            val featureName = feature.name
            val optionalValue = if (ifValue.isNotBlank()) ", \"$ifValue\"" else ""
            println("@Disables(LanguageFeature.$featureName$optionalValue)")
        }
        is Deprecated if kind == AnnotationKind.Gradle -> {
            print("@Deprecated(")
            val hasReplaceWith = annotation.replaceWith.expression.isNotBlank()
            val hasLevel = annotation.level != DeprecationLevel.WARNING
            if (hasReplaceWith || hasLevel) {
                println()
                withIndent {
                    println("message = \"${annotation.message}\",")
                    if (hasLevel) {
                        println("level = DeprecationLevel.${annotation.level.name},")
                    }
                    if (hasReplaceWith) {
                        println("replaceWith = ReplaceWith(")
                        withIndent {
                            println("expression = \"${annotation.replaceWith.expression}\",")
                            println("imports = arrayOf(${annotation.replaceWith.imports.joinToString { "\"$it\"" }}),")
                        }
                    }
                }
            } else {
                print("\"${annotation.message}\")")
            }
            println()
        }
    }
}

private fun SmartPrinter.generateProperty(argument: KotlinCompilerArgument) {
    val name = argument.calculateName()
    val type = when (val type = argument.valueType) {
        is BooleanType -> when (type.isNullable.current) {
            true -> "Boolean?"
            false -> "Boolean"
        }
        is StringArrayType -> "Array<String>?"
        else -> when (type.isNullable.current) {
            true -> "String?"
            false -> "String"
        }
    }
    println("var $name: $type = ${argument.defaultValueInArgs}")
    generateSetter(type, argument)
}

fun KotlinCompilerArgument.calculateName(): String = compilerName ?: name
    .removePrefix("X").removePrefix("X")
    .split("-").joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
    .replaceFirstChar(Char::lowercaseChar)

private fun SmartPrinter.generateSetter(type: String, argument: KotlinCompilerArgument?) {
    withIndent {
        println("set(value) {")
        withIndent {
            println("checkFrozen()")
            if (type == "String?") {
                println("field = if (value.isNullOrEmpty()) ${argument?.defaultValueInArgs} else value")
            } else {
                println("field = value")
            }
        }
        println("}")
    }
}

private fun SmartPrinter.generateConfigurator(info: ArgumentsInfo) {
    if (info.isCommonToolsArgs || !(info.isCommonCompilerArgs || info.levelIsFinal)) return
    println("@get:Transient")
    if (info.levelIsFinal) {
        println("@field:kotlin.jvm.Transient")
    }
    if (info.isCommonCompilerArgs) {
        print("abstract ")
    } else {
        print("override ")
    }
    print("val configurator: CommonCompilerArgumentsConfigurator")
    if (info.levelIsFinal) {
        println(" = ${info.configuratorName}()")
    } else {
        println()
    }
    println()
}

private fun SmartPrinter.generateCopyOf(info: ArgumentsInfo) {
    if (!info.levelIsFinal) return
    val className = info.className
    println("override fun copyOf(): Freezable = copy$className(this, $className())")
}

private fun SmartPrinter.generateDummyImpl() {
    println("// Used only for serialize and deserialize settings. Don't use in other places!")
    println("class DummyImpl : CommonCompilerArguments() {")
    withIndent {
        println("override fun copyOf(): Freezable = copyCommonCompilerArguments(this, DummyImpl())")
        println()
        println("@get:Transient")
        println("@field:kotlin.jvm.Transient")
        println("override val configurator: CommonCompilerArgumentsConfigurator = CommonCompilerArgumentsConfigurator()")
    }
    println("}")
}

private fun SmartPrinter.generateFreeArgsAndErrors() {
    println("var freeArgs: List<String> = emptyList()")
    generateSetter("List<String>", argument = null)
    println()
    println("var internalArguments: List<ManualLanguageFeatureSetting> = emptyList()")
    generateSetter("List<ManualLanguageFeatureSetting>", argument = null)
    println()
    println("@Transient")
    println("var errors: ArgumentParseErrors? = null")
    println()
}

private val KotlinCompilerArgument.defaultValueInArgs: String
    get() {
        @Suppress("UNCHECKED_CAST")
        val valueType = valueType as KotlinArgumentValueType<Any>
        return valueType.stringRepresentation(valueType.defaultValue.current) ?: "null"
    }

private const val tripleQuote = "\"\"\""
