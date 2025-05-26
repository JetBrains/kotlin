/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.arguments.generator

import com.squareup.kotlinpoet.*
import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.types.BooleanType
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.arguments.dsl.types.StringArrayType
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.generators.kotlinpoet.*
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import java.io.File
import java.io.Serializable

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
    val classPackage: String = "org.jetbrains.kotlin.cli.common.arguments",
    val configuratorName: String? = "${className}Configurator",
    val levelIsFinal: Boolean,
    val originFileName: String = className,
    val additionalSyntheticArguments: List<String> = emptyList(),
    val additionalGenerator: TypeSpec.Builder.() -> Unit = {},
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
        additionalGenerator = TypeSpec.Builder::generateFreeArgsAndErrors,
    ),
    ArgumentsInfo(
        levelName = CompilerArgumentsLevelNames.commonCompilerArguments,
        className = "CommonCompilerArguments",
        levelIsFinal = false,
        additionalSyntheticArguments = listOf("autoAdvanceLanguageVersion", "autoAdvanceApiVersion"),
        additionalGenerator = TypeSpec.Builder::generateDummyImpl,
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
        levelName = CompilerArgumentsLevelNames.metadataArguments,
        className = "K2MetadataCompilerArguments",
        levelIsFinal = true,
        originFileName = "MetadataCompilerArguments",
    ),
).associateBy { it.levelName }

private fun generateArgumentsClass(
    genDir: File,
    level: KotlinCompilerArgumentsLevel,
    parent: KotlinCompilerArgumentsLevel?,
) {
    val info = levelToClassNameMap.getValue(level.name)

    val packagePath = info.classPackage.split(".")
    var dir = genDir
    for (packagePart in packagePath) {
        dir = dir.resolve(packagePart)
    }
    dir.mkdirs()
    val file = dir.resolve(info.className + ".kt")

    val builder = StringBuilder().apply { appendLine(COPYRIGHT) }
    generateArgumentsClass(level, parent, info).writeTo(builder)
    file.writeText(builder.toString())
}

private const val ARGUMENTS_PACKAGE = "org.jetbrains.kotlin.cli.common.arguments"

private fun generateArgumentsClass(
    level: KotlinCompilerArgumentsLevel,
    parent: KotlinCompilerArgumentsLevel?,
    info: ArgumentsInfo,
): FileSpec {
    return FileSpec.builder(ARGUMENTS_PACKAGE, info.className).addType(
        TypeSpec.classBuilder(info.className).apply {
            addKdoc(GeneratorsFileUtil.GENERATED_MESSAGE_PREFIX.removePrefix("// ") + "generator in :compiler:cli:cli-arguments-generator\n")
            addKdoc("Please declare arguments in $ORIGIN_FILE_PATH/${info.originFileName}.kt\n")
            addKdoc(GeneratorsFileUtil.GENERATED_MESSAGE_SUFFIX.removePrefix("// "))
            if (!info.levelIsFinal) {
                addModifiers(KModifier.ABSTRACT)
            }

            when (parent) {
                null -> {
                    superclass(ClassName(ARGUMENTS_PACKAGE, "Freezable"))
                    addSuperinterface(Serializable::class)
                }
                else -> superclass(ClassName(ARGUMENTS_PACKAGE, levelToClassNameMap.getValue(parent.name).className))
            }

            generateAdditionalSyntheticArguments(info)
            for (argument in level.arguments) {
                if (argument.releaseVersionsMetadata.removedVersion != null) continue
                generateProperty(argument)
            }
            generateConfigurator(info)
            generateCopyOf(info)
            info.additionalGenerator.invoke(this)
        }.build()
    ).build()
}

private fun TypeSpec.Builder.generateAdditionalSyntheticArguments(info: ArgumentsInfo) {
    for (argument in info.additionalSyntheticArguments) {
        property<Boolean>(argument) {
            annotation<com.intellij.util.xmlb.annotations.Transient> { useSiteTarget(AnnotationSpec.UseSiteTarget.GET) }
            initializer("true")
            generateSetter(typeNameOf<Boolean>())
        }
    }
}

private fun PropertySpec.Builder.generateArgumentAnnotation(argument: KotlinCompilerArgument) {
    annotation(ClassName(ARGUMENTS_PACKAGE, "Argument")) {
        addMember("value = %S", "-${argument.name}")
        argument.shortName?.let { addMember("shortName = %S", "-$it") }
        argument.deprecatedName?.let { addMember("deprecatedName = %S", "-$it") }
        argument.valueDescription.current?.let { addMember("valueDescription = %S", it) }
        argument.delimiter?.let {
            addMember(
                "delimiter = %T.${it.constantName}", ClassName(ARGUMENTS_PACKAGE, "Argument", "Delimiters")
            )
        }
        if (argument.isObsolete) {
            addMember("isObsolete = true")
        }
        addMember("description = %S", argument.description.current)
    }
}

private enum class AnnotationKind {
    Gradle, LanguageFeature
}

private fun PropertySpec.Builder.generateGradleAnnotations(argument: KotlinCompilerArgument) {
    generateAdditionalAnnotations(argument, kind = AnnotationKind.Gradle)
}

private fun PropertySpec.Builder.generateFeatureAnnotations(argument: KotlinCompilerArgument) {
    generateAdditionalAnnotations(argument, kind = AnnotationKind.LanguageFeature)
}

private fun PropertySpec.Builder.generateAdditionalAnnotations(argument: KotlinCompilerArgument, kind: AnnotationKind) {
    for (annotation in argument.additionalAnnotations) {
        generateAnnotation(annotation, kind)
    }
}

private fun PropertySpec.Builder.generateAnnotation(annotation: Annotation, kind: AnnotationKind) {
    val languageFeatureType = LanguageFeature::class.asTypeName()
    when (annotation) {
        is Enables if kind == AnnotationKind.LanguageFeature -> {
            val feature = annotation.feature
            val featureName = feature.name
            annotation<Enables> {
                addMember("%M", MemberName(languageFeatureType, featureName))
            }
        }
        is Disables if kind == AnnotationKind.LanguageFeature -> {
            val feature = annotation.feature
            val featureName = feature.name
            annotation<Disables> {
                addMember("%M", MemberName(languageFeatureType, featureName))
            }
        }
        is GradleOption if kind == AnnotationKind.Gradle -> {
            annotation<GradleOption> {
                addMember("value = %M", MemberName(DefaultValue::class.asClassName(), annotation.value.name))
                addMember("gradleInputType = %M", MemberName(GradleInputTypes::class.asClassName(), annotation.gradleInputType.name))

                if (annotation.shouldGenerateDeprecatedKotlinOptions) {
                    addMember("shouldGenerateDeprecatedKotlinOptions = true")
                }
                if (annotation.gradleName != "") {
                    addMember("gradleName = %S", annotation.gradleName)
                }
            }
        }
        is GradleDeprecatedOption if kind == AnnotationKind.Gradle -> {
            annotation<GradleDeprecatedOption> {
                addMember("message = %S", annotation.message)
                addMember("removeAfter = %M", MemberName(LanguageVersion::class.asClassName(), annotation.removeAfter.name))
                addMember("level = %M", MemberName(DeprecationLevel::class.asClassName(), annotation.level.name))
            }
        }
        is Deprecated if kind == AnnotationKind.Gradle -> {
            annotation<Deprecated> {
                val hasReplaceWith = annotation.replaceWith.expression.isNotBlank()
                val hasLevel = annotation.level != DeprecationLevel.WARNING
                if (hasReplaceWith || hasLevel) {
                    addMember("message = %S", annotation.message)
                    if (hasLevel) {
                        addMember("level = %M", MemberName(DeprecationLevel::class.asClassName(), annotation.level.name))
                    }
                    if (hasReplaceWith) {
                        addMember(
                            "replaceWith = %L",
                            AnnotationSpec.builder(ReplaceWith::class)
                                .addMember("expression = %S", annotation.replaceWith.expression)
                                .addMember(CodeBlock.builder().add("imports = [").apply {
                                    add(annotation.replaceWith.imports.joinToString {
                                        CodeBlock.of("%S", it).toString()
                                    })
                                }.add("]").build())
                                .build()
                        )
                    }
                } else {
                    addMember("%S", annotation.message)
                }
            }
        }
    }
}

private fun TypeSpec.Builder.generateProperty(argument: KotlinCompilerArgument) {
    val name = argument.compilerName ?: argument.name.removePrefix("X").removePrefix("X").split("-")
        .joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }.replaceFirstChar(Char::lowercaseChar)
    val type = when (val type = argument.valueType) {
        is BooleanType -> typeNameOf<Boolean>().copy(nullable = type.isNullable.current)
        is StringArrayType -> arrayTypeNameOf<String>().toNullable()
        else -> typeNameOf<String>().copy(nullable = type.isNullable.current)
    }
    property(name, type) {
        initializer(argument.defaultValueInArgs)
        generateSetter(type, argument)
        generateGradleAnnotations(argument)
        generateArgumentAnnotation(argument)
        generateFeatureAnnotations(argument)
    }
}

private fun PropertySpec.Builder.generateSetter(typeName: TypeName, argument: KotlinCompilerArgument? = null): PropertySpec.Builder {
    return mutable().setter("value") {
        addStatement("checkFrozen()")
        addStatement(
            if (typeName == typeNameOf<String?>()) {
                "field = if (value.isNullOrEmpty()) ${argument?.defaultValueInArgs} else value"
            } else {
                "field = value"
            }
        )
    }
}

private fun TypeSpec.Builder.generateConfigurator(info: ArgumentsInfo) {
    if (info.isCommonToolsArgs || !(info.isCommonCompilerArgs || info.levelIsFinal)) return
    property("configurator", ClassName(ARGUMENTS_PACKAGE, "CommonCompilerArgumentsConfigurator")) {
        annotation<com.intellij.util.xmlb.annotations.Transient> {
            useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
        }
        if (info.levelIsFinal) {
            annotation<Transient> { useSiteTarget(AnnotationSpec.UseSiteTarget.FIELD) }
            initializer("%T()", ClassName(ARGUMENTS_PACKAGE, info.configuratorName!!))
        }
        if (info.isCommonCompilerArgs) {
            addModifiers(KModifier.ABSTRACT)
        } else {
            addModifiers(KModifier.OVERRIDE)
        }
    }
}

private fun TypeSpec.Builder.generateCopyOf(info: ArgumentsInfo) {
    if (!info.levelIsFinal) return
    val className = info.className
    function("copyOf") {
        addModifiers(KModifier.OVERRIDE)
        returns(ClassName(ARGUMENTS_PACKAGE, "Freezable"))
        addStatement("return %M(this, %T())", MemberName(info.classPackage, "copy$className"), ClassName(info.classPackage, className))
    }
}

private fun TypeSpec.Builder.generateDummyImpl() {
    addType(
        TypeSpec.classBuilder("DummyImpl").addKdoc("Used only for serialize and deserialize settings. Don't use in other places!")
            .superclass(ClassName(ARGUMENTS_PACKAGE, "CommonCompilerArguments"))
            .function("copyOf") {
                addModifiers(KModifier.OVERRIDE)
                returns(ClassName(ARGUMENTS_PACKAGE, "Freezable"))
                addStatement("return %M(this, DummyImpl())", MemberName(ARGUMENTS_PACKAGE, "copyCommonCompilerArguments"))
            }.property(
                "configurator", ClassName(ARGUMENTS_PACKAGE, "CommonCompilerArgumentsConfigurator"), KModifier.OVERRIDE
            ) {
                initializer("%T()", ClassName(ARGUMENTS_PACKAGE, "CommonCompilerArgumentsConfigurator"))
                annotation<Transient> { useSiteTarget(AnnotationSpec.UseSiteTarget.FIELD) }
                annotation<com.intellij.util.xmlb.annotations.Transient> {
                    useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                }
            }.build()
    )
}

private fun TypeSpec.Builder.generateFreeArgsAndErrors() {
    property("freeArgs", listTypeNameOf<String>()) {
        initializer("%M()", MemberName("kotlin.collections", "emptyList"))
        generateSetter(listTypeNameOf<String>())
        mutable()
    }

    property(
        "internalArguments", listTypeNameOf(ClassName(ARGUMENTS_PACKAGE, "InternalArgument"))
    ) {
        initializer("%M()", MemberName("kotlin.collections", "emptyList"))
        generateSetter(listTypeNameOf<String>())
        mutable()
    }


    property(
        "errors", ClassName(ARGUMENTS_PACKAGE, "ArgumentParseErrors").toNullable()
    ) {
        initializer("null")
        annotation<Transient> { }
        mutable()
    }
}

private val KotlinCompilerArgument.defaultValueInArgs: String
    get() {
        @Suppress("UNCHECKED_CAST") val valueType = valueType as KotlinArgumentValueType<Any?>
        return valueType.stringRepresentation(valueType.defaultValue.current) ?: "null"
    }
