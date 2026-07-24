/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.arguments.generator

import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.ExperimentalArgumentApi
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.base.Modifier
import org.jetbrains.kotlin.arguments.dsl.types.*
import org.jetbrains.kotlin.cli.common.arguments.Disables
import org.jetbrains.kotlin.cli.common.arguments.Enables
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

private val COPYRIGHT by lazy { File("license/COPYRIGHT_HEADER.txt").readText() }
private const val ORIGIN_FILE_PATH = "compiler/arguments/src/org/jetbrains/kotlin/arguments/description"

fun main(args: Array<String>) {
    val genDir = File(args[0])

    val alreadyRemovedArguments = mutableSetOf<KotlinCompilerArgument>()
    for (level in args.drop(1)) {
        val [level, parent] = findLevelWithParent(level)
        generateArgumentsClass(genDir, level, parent)

        for (argument in level.arguments) {
            if (argument.isAlreadyRemoved) {
                alreadyRemovedArguments.add(argument)
            }
        }
    }

    val removedArgumentsLevel = KotlinCompilerArgumentsLevel(
        name = removedCompilerArgumentsSpecialLevelName,
        arguments = alreadyRemovedArguments,
        nestedLevels = emptySet(),
        modifiers = setOf(Modifier.DEPRECATED),
    )
    generateArgumentsClass(genDir, removedArgumentsLevel, parent = null)
}

private fun findLevelWithParent(name: String): Pair<KotlinCompilerArgumentsLevel, KotlinCompilerArgumentsLevel?> {
    fun find(
        level: KotlinCompilerArgumentsLevel,
        parent: KotlinCompilerArgumentsLevel?,
    ): Pair<KotlinCompilerArgumentsLevel, KotlinCompilerArgumentsLevel?>? {
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
        levelName = removedCompilerArgumentsSpecialLevelName,
        className = "RemovedCompilerArguments",
        levelIsFinal = true,
    ),
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
        levelName = CompilerArgumentsLevelNames.legacyWasmArguments,
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
        levelName = CompilerArgumentsLevelNames.metadataArguments,
        className = "K2MetadataCompilerArguments",
        levelIsFinal = true,
        originFileName = "MetadataCompilerArguments",
    ),
    ArgumentsInfo(
        levelName = CompilerArgumentsLevelNames.commonJsAndWasmArguments,
        className = "CommonJsAndWasmCompilerArguments",
        levelIsFinal = false,
        originFileName = "CommonJsAndWasmCompilerArguments",
    ),
    ArgumentsInfo(
        levelName = CompilerArgumentsLevelNames.wasmArguments,
        className = "KotlinWasmCompilerArguments",
        levelIsFinal = true,
        originFileName = "KotlinWasmCompilerArguments",
    ),
).associateBy { it.levelName }

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
    info: ArgumentsInfo,
) {
    val generateAlreadyRemovedArguments = info.levelName == removedCompilerArgumentsSpecialLevelName

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
    if (!generateAlreadyRemovedArguments) {
        println("// Please declare arguments in $ORIGIN_FILE_PATH/${info.originFileName}.kt")
    }
    println(GeneratorsFileUtil.GENERATED_MESSAGE_SUFFIX)
    println()

    if (Modifier.DEPRECATED in level.modifiers) {
        val message: String
        val level: DeprecationLevel
        if (generateAlreadyRemovedArguments) {
            message = "This class exists solely to facilitate detailed error reporting."
            level = DeprecationLevel.ERROR
        } else {
            message = "This class was deprecated and will be removed soon."
            level = DeprecationLevel.WARNING
        }
        println("@Deprecated(\"$message\", level = DeprecationLevel.$level)")
    }
    if (Modifier.DEPRECATED in (parent?.modifiers ?: emptySet())) {
        println("@Suppress(\"DEPRECATION\")")
    }
    if (!info.levelIsFinal) {
        if (Modifier.SEALED in level.modifiers) {
            print("sealed ")
        } else {
            print("abstract ")
        }
    }
    print("class ${info.className}")
    if (!generateAlreadyRemovedArguments) {
        val supertypes = when (parent) {
            null -> "Freezable(), Serializable"
            else -> "${levelToClassNameMap.getValue(parent.name).className}()"
        }
        print(" : $supertypes")
    }
    println(" {")
    withIndent {
        generateAdditionalSyntheticArguments(info)
        for (argument in level.arguments) {
            if (generateAlreadyRemovedArguments xor argument.isAlreadyRemoved) {
                continue
            }

            validateLifetime(argument)
            validateLanguageFeaturesConsistency(argument)
            generateDeprecationAnnotation(argument)
            generateGradleAnnotations(argument)
            generateArgumentAnnotation(argument, level)
            generateFeatureAnnotations(argument)
            generateProperty(argument)
            println()
        }
        if (!generateAlreadyRemovedArguments) {
            generateConfigurator(info)
            generateCopyOf(info)
        }
        info.additionalGenerator.invoke(this)
    }
    println("}")
}

private fun KotlinCompilerArgumentsLevel.collectImports(info: ArgumentsInfo): List<String> {
    val rawImports = buildSet {
        add("org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArgumentsConfigurator")
        if ((info.levelIsFinal || info.isCommonCompilerArgs) && info.levelName != removedCompilerArgumentsSpecialLevelName) {
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
        if (arguments.any { arg -> arg.defaultValueInArgs.contains(File::class.simpleName!!) }) {
            add(File::class.qualifiedName!!)
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

        argument.releaseVersionsMetadata.deprecatedVersion?.let { println("deprecatedVersion = \"${it.releaseName}\",") }
        argument.releaseVersionsMetadata.removedVersion?.let { println("removedVersion = \"${it.releaseName}\",") }
    }
    println(")")
}

private enum class AnnotationKind {
    Gradle,
    LanguageFeature
}

private fun validateLifetime(argument: KotlinCompilerArgument) {
    argument.releaseVersionsMetadata.apply {
        var maxVersion = introducedVersion

        stabilizedVersion?.let {
            require(it >= introducedVersion) { "Stabilized version must be >= introduced version for '${argument.name}'" }
            maxVersion = it
        }

        deprecatedVersion?.let {
            // Actually, it should be strictly `>`, but we have some arguments that became deprecated right after becoming introduced/stabilized
            require(it >= maxVersion) { "Deprecated version must be >= introduced and stabilized versions for '${argument.name}'" }
            maxVersion = it
        }

        removedVersion?.let {
            require(it > maxVersion) { "Removed version must be > introduced, stabilized, and deprecated versions for '${argument.name}'" }
            maxVersion = it
        }

        require(maxVersion.toKotlinVersion() <= kotlin.KotlinVersion.CURRENT) {
            "Max version '{$maxVersion}' (introduced/stabilized/deprecated/removed) for '${argument.name}' cannot be in the future (must be <= current version). " +
                    "Use the current version '${kotlin.KotlinVersion.CURRENT}' or postpone the change (create an issue and add a TODO comment that references that issue)."
        }
    }
}

@OptIn(ExperimentalArgumentApi::class)
private fun validateLanguageFeaturesConsistency(argument: KotlinCompilerArgument) {
    if (argument.additionalAnnotations.none { it is Enables || it is Disables }) return

    fun Annotation.getIfValueIs(): String? = when (this) {
        is Enables -> ifValueIs
        is Disables -> ifValueIs
        else -> null
    }

    when (val argumentType = argument.argumentType) {
        is BooleanType -> {
            val defaultValue = argumentType.defaultValue.current
            if (defaultValue != false) {
                error("Argument '${argument.name}' has Boolean type and changes language features. Expected default value is 'false', but actual is '$defaultValue'.")
            }
            for (additionalAnn in argument.additionalAnnotations) {
                val ifValueIs = additionalAnn.getIfValueIs() ?: continue
                if (ifValueIs.isNotEmpty()) {
                    error("Argument '${argument.name}' has Boolean type and changes language features. It's expected that 'ifValueIs' isn't set, but actually it's '$ifValueIs'.")
                }
            }
        }
        is AnnotationDefaultTargetModeType,
        is NameBasedDestructuringModeType
            -> {
            val defaultValue = argument.argumentType.defaultValue.current
            val typeName = argument.argumentType::class.simpleName
            if (defaultValue != null) {
                error("Argument '${argument.name}' has $typeName type and changes language features. Expected default value is 'null', but actual is '$defaultValue'")
            }
            for (additionalAnn in argument.additionalAnnotations) {
                val ifValueIs = additionalAnn.getIfValueIs() ?: continue
                if (ifValueIs.isEmpty()) {
                    error("Argument '${argument.name}' has $typeName type and changes language features. Non-empty 'ifValueIs' is expected.")
                }
            }
        }
        else -> {
            error(
                "Unexpected type for argument '${argument.name}' that changes language features: ${argumentType::class.simpleName}. " +
                        "Allowed types: ${BooleanType::class.simpleName}, ${AnnotationDefaultTargetModeType::class.simpleName}, ${NameBasedDestructuringModeType::class.simpleName}."
            )
        }
    }
}

fun SmartPrinter.generateDeprecationAnnotation(argument: KotlinCompilerArgument) {
    if (argument.additionalAnnotations.any { it is Deprecated }) {
        error("Remove deprecated annotation for '${argument.name}' because it's generated automatically based on 'deprecatedVersion' and 'deprecatedMessage'")
    }

    val releaseVersionsMetadata = argument.releaseVersionsMetadata
    if (releaseVersionsMetadata.deprecatedVersion == null && argument.deprecatedMessage != null) {
        error("Deprecated message is specified for argument '${argument.name}' but deprecated version is not set")
    }

    val deprecationLevel = when {
        argument.isAlreadyRemoved -> {
            DeprecationLevel.ERROR
        }
        releaseVersionsMetadata.deprecatedVersion.let { it != null && it.toKotlinVersion() <= kotlin.KotlinVersion.CURRENT } -> {
            DeprecationLevel.WARNING
        }
        else -> {
            null
        }
    }
    if (deprecationLevel != null) {
        // Mark deprecated/removed arguments with warning/error deprecation level according to the specified version.
        generateAnnotation(
            Deprecated(
                message = argument.deprecatedMessage ?: "",
                level = deprecationLevel,
            ),
            kind = AnnotationKind.Gradle,
        )
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
        is Disables if kind == AnnotationKind.LanguageFeature -> {
            val feature = annotation.feature
            val ifValue = annotation.ifValueIs
            val featureName = feature.name
            val optionalValue = if (ifValue.isNotBlank()) ", \"$ifValue\"" else ""
            println("@Disables(LanguageFeature.$featureName$optionalValue)")
        }
        is Deprecated if kind == AnnotationKind.Gradle -> {
            print("@all:Deprecated(")
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
                print(')')
            } else {
                print("\"${annotation.message}\")")
            }
            println()
        }
    }
}

@OptIn(ExperimentalArgumentApi::class)
private fun SmartPrinter.generateProperty(argument: KotlinCompilerArgument) {
    val name = argument.calculateName()
    val type = when (val type = argument.argumentType) {
        is BooleanType -> when (type.isNullable.current) {
            true -> "Boolean?"
            false -> "Boolean"
        }
        is StringArrayType -> "Array<String>"
        is StringListType -> "Array<String>"
        is SearchPathType -> "String?"
        is PathListType -> "Array<String>"
        is EnumListType<*> -> "Array<String>"
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
            if (argument?.isAlreadyRemoved != true) {
                println("checkFrozen()")
            }
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
    println("@Transient")
    println("var explicitArguments: Map<ArgumentField, List<Any>> = emptyMap()")
    println()
}

@OptIn(ExperimentalArgumentApi::class)
private val KotlinCompilerArgument.defaultValueInArgs: String
    get() {
        return when (@Suppress("UNCHECKED_CAST") val valueType = argumentType as KotlinArgumentValueType<Any>) {
            is EnumListType<*> -> "emptyArray()"
            is StringArrayType -> "emptyArray()"
            is StringListType if valueType.defaultValue.current.isNullOrEmpty() -> "emptyArray()"
            is StringListType -> "arrayOf(${valueType.stringRepresentation(valueType.defaultValue.current)})"
            is PathListType if valueType.defaultValue.current.isNullOrEmpty() -> "emptyArray()"
            is PathListType -> "arrayOf(${valueType.stringRepresentation(valueType.defaultValue.current)})"
            else -> valueType.stringRepresentation(valueType.defaultValue.current) ?: "null"
        }
    }

private const val tripleQuote = "\"\"\""

/**
 * Represents the special level name solely for removed compiler arguments.
 *
 * This constant is used to identify a specific `KotlinCompilerArgumentsLevel`
 * that aggregates compiler arguments marked as removed. It acts as a unique identifier.
 */
private const val removedCompilerArgumentsSpecialLevelName = "removedCompilerArguments"

private val KotlinCompilerArgument.isAlreadyRemoved: Boolean
    get() = releaseVersionsMetadata.removedVersion.let { it != null && it.toKotlinVersion() <= kotlin.KotlinVersion.CURRENT }

