/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.arguments

import org.jetbrains.kotlin.arguments.description.*
import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.arguments.dsl.types.StringArrayType
import org.jetbrains.kotlin.cli.arguments.generator.calculateName
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.withNullability

private val additionalGradleProperties = KotlinCompilerArgument(
    name = "freeCompilerArgs",
    description = ReleaseDependent("A list of additional compiler arguments", valueInVersions = emptyMap()),
    delimiter = KotlinCompilerArgument.Delimiter.None,
    valueType = StringArrayType.defaultNull,
    releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
        introducedVersion = KotlinReleaseVersion.v1_4_0,
        stabilizedVersion = KotlinReleaseVersion.v1_4_0,
    ),
    additionalMetadata = listOf(
        GradleOption(
            value = DefaultValue.EMPTY_STRING_LIST_DEFAULT,
            gradleInputType = GradleInputTypes.INPUT,
            shouldGenerateDeprecatedKotlinOptions = true,
        )
    )
)

private data class GeneratedOptions(
    val optionsName: FqName,
    val deprecatedOptionsName: FqName?,
    val properties: List<KotlinCompilerArgument>,
)

private data class GeneratedImplOptions(
    val baseImplName: FqName,
    val helperName: FqName,
)

private const val GRADLE_API_SRC_DIR = "libraries/tools/kotlin-gradle-plugin-api/src/common/kotlin"
private const val GRADLE_PLUGIN_SRC_DIR = "libraries/tools/kotlin-gradle-plugin/src/common/kotlin"
private const val OPTIONS_PACKAGE_PREFIX = "org.jetbrains.kotlin.gradle.dsl"
private const val IMPLEMENTATION_SUFFIX = "Default"
private const val IMPLEMENTATION_HELPERS_SUFFIX = "Helper"

private const val TOOL_OPTIONS_KDOC = "Common options for all Kotlin platforms' compilations and tools."
private const val COMMON_COMPILER_OPTIONS_KDOC = "Common compiler options for all Kotlin platforms."
private const val JVM_COMPILER_OPTIONS_KDOC = "Compiler options for Kotlin/JVM."
private const val JS_COMPILER_OPTIONS_KDOC = "Compiler options for Kotlin/JS."
private const val NATIVE_COMPILER_OPTIONS_KDOC = "Compiler options for Kotlin Native."
private const val MULTIPLATFORM_COMPILER_OPTION_KDOC = "Compiler options for the Kotlin common platform."

fun generateKotlinGradleOptions(withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit) {
    val apiSrcDir = File(GRADLE_API_SRC_DIR)
    val srcDir = File(GRADLE_PLUGIN_SRC_DIR)

    val commonToolOptions = generateKotlinCommonToolOptions(apiSrcDir, withPrinterToFile)
    val commonToolImplOptions = generateKotlinCommonToolOptionsImpl(
        srcDir,
        commonToolOptions.optionsName,
        commonToolOptions.properties,
        withPrinterToFile
    )

    val commonCompilerOptions = generateKotlinCommonOptions(
        apiSrcDir,
        commonToolOptions,
        withPrinterToFile
    )
    val commonCompilerOptionsImpl = generateKotlinCommonOptionsImpl(
        srcDir,
        commonCompilerOptions.optionsName,
        commonToolImplOptions.baseImplName,
        commonToolImplOptions.helperName,
        commonCompilerOptions.properties,
        withPrinterToFile
    )

    val jvmOptions = generateKotlinJvmOptions(
        apiSrcDir,
        commonCompilerOptions,
        withPrinterToFile
    )
    generateKotlinJvmOptionsImpl(
        srcDir,
        jvmOptions.optionsName,
        commonCompilerOptionsImpl.baseImplName,
        commonCompilerOptionsImpl.helperName,
        jvmOptions.properties,
        withPrinterToFile
    )

    val jsOptions = generateKotlinJsOptions(
        apiSrcDir,
        commonCompilerOptions,
        withPrinterToFile
    )
    generateKotlinJsOptionsImpl(
        srcDir,
        jsOptions.optionsName,
        commonCompilerOptionsImpl.baseImplName,
        commonCompilerOptionsImpl.helperName,
        jsOptions.properties,
        withPrinterToFile
    )

    val nativeOptions = generateKotlinNativeOptions(
        apiSrcDir,
        commonCompilerOptions,
        withPrinterToFile
    )
    generateKotlinNativeOptionsImpl(
        srcDir,
        nativeOptions.optionsName,
        commonCompilerOptionsImpl.baseImplName,
        commonCompilerOptionsImpl.helperName,
        nativeOptions.properties,
        withPrinterToFile
    )

    val multiplatformCommonOptions = generateMultiplatformCommonOptions(
        apiSrcDir,
        commonCompilerOptions,
        withPrinterToFile
    )
    generateMultiplatformCommonOptionsImpl(
        srcDir,
        multiplatformCommonOptions.optionsName,
        commonCompilerOptionsImpl.baseImplName,
        commonCompilerOptionsImpl.helperName,
        multiplatformCommonOptions.properties,
        withPrinterToFile
    )
}

fun main() {
    generateKotlinGradleOptions(::getPrinterToFile)
}

private fun generateKotlinCommonToolOptions(
    apiSrcDir: File,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit,
): GeneratedOptions {
    val commonInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinCommonCompilerToolOptions")
    val commonOptions = actualCommonToolsArguments.gradleOptions()
    val additionalOptions = additionalGradleProperties
    withPrinterToFile(fileFromFqName(apiSrcDir, commonInterfaceFqName)) {
        generateInterface(
            commonInterfaceFqName,
            commonOptions + additionalOptions,
            interfaceKDoc = TOOL_OPTIONS_KDOC,
        )
    }

    val deprecatedCommonInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinCommonToolOptions")
    withPrinterToFile(fileFromFqName(apiSrcDir, deprecatedCommonInterfaceFqName)) {
        generateDeprecatedInterface(
            deprecatedCommonInterfaceFqName,
            commonInterfaceFqName,
            commonOptions + additionalOptions,
            parentType = null,
            interfaceKDoc = TOOL_OPTIONS_KDOC,
        )
    }

    println("### Attributes common for JVM, JS, and JS DCE\n")
    generateMarkdown(commonOptions + additionalOptions)

    return GeneratedOptions(commonInterfaceFqName, deprecatedCommonInterfaceFqName, (commonOptions + additionalOptions))
}

private fun generateKotlinCommonToolOptionsImpl(
    srcDir: File,
    commonToolOptionsInterfaceFqName: FqName,
    options: List<KotlinCompilerArgument>,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit,
): GeneratedImplOptions {
    val commonToolBaseImplFqName = FqName("${commonToolOptionsInterfaceFqName.asString()}$IMPLEMENTATION_SUFFIX")
    withPrinterToFile(fileFromFqName(srcDir, commonToolBaseImplFqName)) {
        generateImpl(
            commonToolBaseImplFqName,
            null,
            commonToolOptionsInterfaceFqName,
            options,
        )
    }

    val k2CommonToolCompilerArgumentsFqName = FqName(CommonToolArguments::class.qualifiedName!!)
    val commonToolCompilerArgsImplFqName = FqName(
        "${commonToolOptionsInterfaceFqName.asString()}$IMPLEMENTATION_HELPERS_SUFFIX"
    )
    withPrinterToFile(fileFromFqName(srcDir, commonToolCompilerArgsImplFqName)) {
        generateCompilerOptionsHelper(
            commonToolOptionsInterfaceFqName,
            commonToolCompilerArgsImplFqName,
            null,
            k2CommonToolCompilerArgumentsFqName,
            options
        )
    }

    return GeneratedImplOptions(commonToolBaseImplFqName, commonToolCompilerArgsImplFqName)
}

private fun generateKotlinCommonOptions(
    apiSrcDir: File,
    commonToolGeneratedOptions: GeneratedOptions,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit,
): GeneratedOptions {
    val commonCompilerInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinCommonCompilerOptions")
    val commonCompilerOptions = actualCommonCompilerArguments.gradleOptions()
    withPrinterToFile(fileFromFqName(apiSrcDir, commonCompilerInterfaceFqName)) {
        generateInterface(
            commonCompilerInterfaceFqName,
            commonCompilerOptions,
            parentType = commonToolGeneratedOptions.optionsName,
            interfaceKDoc = COMMON_COMPILER_OPTIONS_KDOC,
        )
    }

    val deprecatedCommonCompilerInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinCommonOptions")
    withPrinterToFile(fileFromFqName(apiSrcDir, deprecatedCommonCompilerInterfaceFqName)) {
        generateDeprecatedInterface(
            deprecatedCommonCompilerInterfaceFqName,
            commonCompilerInterfaceFqName,
            commonCompilerOptions,
            parentType = commonToolGeneratedOptions.deprecatedOptionsName,
            interfaceKDoc = COMMON_COMPILER_OPTIONS_KDOC,
        )
    }

    println("\n### Attributes common for JVM and JS\n")
    generateMarkdown(commonCompilerOptions)

    return GeneratedOptions(commonCompilerInterfaceFqName, deprecatedCommonCompilerInterfaceFqName, commonCompilerOptions)
}

private fun generateKotlinCommonOptionsImpl(
    srcDir: File,
    commonOptionsInterfaceFqName: FqName,
    commonToolImpl: FqName,
    commonToolCompilerHelperName: FqName,
    options: List<KotlinCompilerArgument>,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit,
): GeneratedImplOptions {
    val commonCompilerImplFqName = FqName("${commonOptionsInterfaceFqName.asString()}$IMPLEMENTATION_SUFFIX")
    withPrinterToFile(fileFromFqName(srcDir, commonCompilerImplFqName)) {
        generateImpl(
            commonCompilerImplFqName,
            commonToolImpl,
            commonOptionsInterfaceFqName,
            options,
        )
    }

    val k2CommonCompilerArgumentsFqName = FqName(CommonCompilerArguments::class.qualifiedName!!)
    val commonCompilerHelperFqName = FqName(
        "${commonOptionsInterfaceFqName.asString()}$IMPLEMENTATION_HELPERS_SUFFIX"
    )
    withPrinterToFile(fileFromFqName(srcDir, commonCompilerHelperFqName)) {
        generateCompilerOptionsHelper(
            commonOptionsInterfaceFqName,
            commonCompilerHelperFqName,
            commonToolCompilerHelperName,
            k2CommonCompilerArgumentsFqName,
            options
        )
    }

    return GeneratedImplOptions(commonCompilerImplFqName, commonCompilerHelperFqName)
}

private fun generateKotlinJvmOptions(
    apiSrcDir: File,
    commonCompilerGeneratedOptions: GeneratedOptions,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit,
): GeneratedOptions {
    val jvmInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinJvmCompilerOptions")
    val jvmOptions = actualJvmCompilerArguments.gradleOptions()
    withPrinterToFile(fileFromFqName(apiSrcDir, jvmInterfaceFqName)) {
        generateInterface(
            jvmInterfaceFqName,
            jvmOptions,
            parentType = commonCompilerGeneratedOptions.optionsName,
            interfaceKDoc = JVM_COMPILER_OPTIONS_KDOC,
        )
    }

    val deprecatedJvmInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinJvmOptions")
    withPrinterToFile(fileFromFqName(apiSrcDir, deprecatedJvmInterfaceFqName)) {
        generateDeprecatedInterface(
            deprecatedJvmInterfaceFqName,
            jvmInterfaceFqName,
            jvmOptions,
            parentType = commonCompilerGeneratedOptions.deprecatedOptionsName,
            interfaceKDoc = JVM_COMPILER_OPTIONS_KDOC,
        )
    }

    println("\n### Attributes specific for JVM\n")
    generateMarkdown(jvmOptions)

    return GeneratedOptions(jvmInterfaceFqName, deprecatedJvmInterfaceFqName, jvmOptions)
}

private fun generateKotlinJvmOptionsImpl(
    srcDir: File,
    jvmInterfaceFqName: FqName,
    commonCompilerImpl: FqName,
    commonCompilerHelperName: FqName,
    jvmOptions: List<KotlinCompilerArgument>,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit,
) {
    val jvmImplFqName = FqName("${jvmInterfaceFqName.asString()}$IMPLEMENTATION_SUFFIX")
    withPrinterToFile(fileFromFqName(srcDir, jvmImplFqName)) {
        generateImpl(
            jvmImplFqName,
            commonCompilerImpl,
            jvmInterfaceFqName,
            jvmOptions
        )
    }

    val k2JvmCompilerArgumentsFqName = FqName(K2JVMCompilerArguments::class.qualifiedName!!)
    val jvmCompilerOptionsHelperFqName = FqName(
        "${jvmInterfaceFqName.asString()}$IMPLEMENTATION_HELPERS_SUFFIX"
    )
    withPrinterToFile(fileFromFqName(srcDir, jvmCompilerOptionsHelperFqName)) {
        generateCompilerOptionsHelper(
            jvmInterfaceFqName,
            jvmCompilerOptionsHelperFqName,
            commonCompilerHelperName,
            k2JvmCompilerArgumentsFqName,
            jvmOptions
        )
    }
}

private fun generateKotlinJsOptions(
    apiSrcDir: File,
    commonCompilerOptions: GeneratedOptions,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit,
): GeneratedOptions {
    val jsInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinJsCompilerOptions")
    val jsOptions = actualJsArguments.gradleOptions()
    withPrinterToFile(fileFromFqName(apiSrcDir, jsInterfaceFqName)) {
        generateInterface(
            jsInterfaceFqName,
            jsOptions,
            parentType = commonCompilerOptions.optionsName,
            interfaceKDoc = JS_COMPILER_OPTIONS_KDOC,
        )
    }

    val deprecatedJsInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinJsOptions")
    withPrinterToFile(fileFromFqName(apiSrcDir, deprecatedJsInterfaceFqName)) {
        generateDeprecatedInterface(
            deprecatedJsInterfaceFqName,
            jsInterfaceFqName,
            jsOptions,
            parentType = commonCompilerOptions.deprecatedOptionsName,
            interfaceKDoc = JS_COMPILER_OPTIONS_KDOC,
        )
    }

    println("\n### Attributes specific for JS\n")
    generateMarkdown(jsOptions)

    return GeneratedOptions(jsInterfaceFqName, deprecatedJsInterfaceFqName, jsOptions)
}

private fun generateKotlinJsOptionsImpl(
    srcDir: File,
    jsInterfaceFqName: FqName,
    commonCompilerImpl: FqName,
    commonCompilerHelperName: FqName,
    jsOptions: List<KotlinCompilerArgument>,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit,
) {
    val jsImplFqName = FqName("${jsInterfaceFqName.asString()}$IMPLEMENTATION_SUFFIX")
    withPrinterToFile(fileFromFqName(srcDir, jsImplFqName)) {
        generateImpl(
            jsImplFqName,
            commonCompilerImpl,
            jsInterfaceFqName,
            jsOptions
        )
    }

    val k2JsCompilerArgumentsFqName = FqName(K2JSCompilerArguments::class.qualifiedName!!)
    val jsCompilerOptionsHelperFqName = FqName(
        "${jsInterfaceFqName.asString()}$IMPLEMENTATION_HELPERS_SUFFIX"
    )
    withPrinterToFile(fileFromFqName(srcDir, jsCompilerOptionsHelperFqName)) {
        generateCompilerOptionsHelper(
            jsInterfaceFqName,
            jsCompilerOptionsHelperFqName,
            commonCompilerHelperName,
            k2JsCompilerArgumentsFqName,
            jsOptions
        )
    }
}

private fun generateKotlinNativeOptions(
    apiSrcDir: File,
    commonCompilerOptions: GeneratedOptions,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit,
): GeneratedOptions {
    val nativeInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinNativeCompilerOptions")
    val nativeOptions = actualNativeArguments.gradleOptions()
    withPrinterToFile(fileFromFqName(apiSrcDir, nativeInterfaceFqName)) {
        generateInterface(
            nativeInterfaceFqName,
            nativeOptions,
            parentType = commonCompilerOptions.optionsName,
            interfaceKDoc = NATIVE_COMPILER_OPTIONS_KDOC
        )
    }

    println("\n### Attributes specific for Native\n")
    generateMarkdown(nativeOptions)

    return GeneratedOptions(nativeInterfaceFqName, null, nativeOptions)
}

private fun generateKotlinNativeOptionsImpl(
    srcDir: File,
    nativeInterfaceFqName: FqName,
    commonCompilerImpl: FqName,
    commonCompilerHelper: FqName,
    nativeOptions: List<KotlinCompilerArgument>,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit,
) {
    val nativeImplFqName = FqName("${nativeInterfaceFqName.asString()}$IMPLEMENTATION_SUFFIX")
    withPrinterToFile(fileFromFqName(srcDir, nativeImplFqName)) {
        generateImpl(
            nativeImplFqName,
            commonCompilerImpl,
            nativeInterfaceFqName,
            nativeOptions
        )
    }

    val k2NativeCompilerArgumentsFqName = FqName(K2NativeCompilerArguments::class.qualifiedName!!)
    val nativeCompilerOptionsHelperFqName = FqName(
        "${nativeInterfaceFqName.asString()}$IMPLEMENTATION_HELPERS_SUFFIX"
    )
    withPrinterToFile(fileFromFqName(srcDir, nativeCompilerOptionsHelperFqName)) {
        generateCompilerOptionsHelper(
            nativeInterfaceFqName,
            nativeCompilerOptionsHelperFqName,
            commonCompilerHelper,
            k2NativeCompilerArgumentsFqName,
            nativeOptions
        )
    }
}

private fun generateMultiplatformCommonOptions(
    apiSrcDir: File,
    commonCompilerOptions: GeneratedOptions,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit,
): GeneratedOptions {
    val multiplatformCommonInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinMultiplatformCommonCompilerOptions")
    val multiplatformCommonOptions = actualMetadataArguments.gradleOptions()
    withPrinterToFile(fileFromFqName(apiSrcDir, multiplatformCommonInterfaceFqName)) {
        generateInterface(
            multiplatformCommonInterfaceFqName,
            multiplatformCommonOptions,
            parentType = commonCompilerOptions.optionsName,
            interfaceKDoc = MULTIPLATFORM_COMPILER_OPTION_KDOC,
        )
    }

    val deprecatedMultiplatformCommonInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinMultiplatformCommonOptions")
    withPrinterToFile(fileFromFqName(apiSrcDir, deprecatedMultiplatformCommonInterfaceFqName)) {
        generateDeprecatedInterface(
            deprecatedMultiplatformCommonInterfaceFqName,
            multiplatformCommonInterfaceFqName,
            parentType = commonCompilerOptions.deprecatedOptionsName,
            properties = multiplatformCommonOptions,
            interfaceKDoc = MULTIPLATFORM_COMPILER_OPTION_KDOC,
        )
    }

    println("\n### Attributes specific for Multiplatform/Common\n")
    generateMarkdown(multiplatformCommonOptions)

    return GeneratedOptions(multiplatformCommonInterfaceFqName, deprecatedMultiplatformCommonInterfaceFqName, multiplatformCommonOptions)
}

private fun generateMultiplatformCommonOptionsImpl(
    srcDir: File,
    multiplatformCommonInterfaceFqName: FqName,
    commonCompilerImpl: FqName,
    commonCompilerHelper: FqName,
    multiplatformCommonOptions: List<KotlinCompilerArgument>,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit,
) {
    val multiplatformCommonImplFqName = FqName("${multiplatformCommonInterfaceFqName.asString()}$IMPLEMENTATION_SUFFIX")
    withPrinterToFile(fileFromFqName(srcDir, multiplatformCommonImplFqName)) {
        generateImpl(
            multiplatformCommonImplFqName,
            commonCompilerImpl,
            multiplatformCommonInterfaceFqName,
            multiplatformCommonOptions
        )
    }

    val k2metadataCompilerArgumentsFqName = FqName(K2MetadataCompilerArguments::class.qualifiedName!!)
    val metadataCompilerHelperFqName = FqName(
        "${multiplatformCommonInterfaceFqName.asString()}$IMPLEMENTATION_HELPERS_SUFFIX"
    )
    withPrinterToFile(fileFromFqName(srcDir, metadataCompilerHelperFqName)) {
        generateCompilerOptionsHelper(
            multiplatformCommonInterfaceFqName,
            metadataCompilerHelperFqName,
            commonCompilerHelper,
            k2metadataCompilerArgumentsFqName,
            multiplatformCommonOptions
        )
    }
}

private fun List<KotlinCompilerArgument>.filterToBeDeleted() = filter { prop ->
    prop.gradleDeprecatedOptionOrNull()
        ?.let { it.removeAfter >= LanguageVersion.LATEST_STABLE }
        ?: true
}

private fun KotlinCompilerArgumentsLevel.gradleOptions(): List<KotlinCompilerArgument> =
    arguments
        .filter {
            it.findMetadata<GradleOption>() != null
        }
        .filterToBeDeleted()
        .sortedBy { it.calculateName() }

internal fun fileFromFqName(baseDir: File, fqName: FqName): File {
    val fileRelativePath = fqName.asString().replace(".", "/") + ".kt"
    return File(baseDir, fileRelativePath)
}

private fun Printer.generateInterface(
    type: FqName,
    properties: List<KotlinCompilerArgument>,
    parentType: FqName? = null,
    interfaceKDoc: String? = null,
) {
    val afterType = parentType?.let { " : $it" }
    generateDeclaration("interface", type, afterType = afterType, declarationKDoc = interfaceKDoc) {
        for (property in properties) {
            println()
            generateDoc(property)
            generateOptionDeprecation(property)
            generatePropertyProvider(property)
        }
    }
}

private fun kotlinOptionDeprecation(
    indent: Int = 0,
    indentFirstLine: Boolean = true,
    deprecationLevel: DeprecationLevel = DeprecationLevel.ERROR,
): String {
    val indentSpaces = generateSequence { " " }.take(indent).joinToString(separator = "")
    val deprecationLevelString = "${DeprecationLevel::class.simpleName}.${deprecationLevel.name}"
    return """
    |${if (indentFirstLine) indentSpaces else ""}@OptIn(org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi::class)
    |$indentSpaces@Deprecated(
    |$indentSpaces    message = org.jetbrains.kotlin.gradle.dsl.KOTLIN_OPTIONS_DEPRECATION_MESSAGE,
    |$indentSpaces    level = ${deprecationLevelString},
    |$indentSpaces)
    """.trimMargin()
}

private fun Printer.generateDeprecatedInterface(
    type: FqName,
    compilerOptionType: FqName,
    properties: List<KotlinCompilerArgument>,
    interfaceKDoc: String? = null,
    parentType: FqName? = null,
) {
    val afterType = parentType?.let { " : $it" }
    val modifier = "${kotlinOptionDeprecation()}\ninterface"
    val deprecatedProperties = properties.filter { it.generateDeprecatedKotlinOption }
    // KotlinMultiplatformCommonOptions doesn't have any options, but it is being kept for backward compatibility
    if (deprecatedProperties.isNotEmpty() || type.asString().endsWith("KotlinMultiplatformCommonOptions")) {
        generateDeclaration(modifier, type, afterType = afterType, declarationKDoc = interfaceKDoc) {

            println()
            println(kotlinOptionDeprecation(indent = 4, indentFirstLine = false))
            println("/**")
            println(" * @suppress")
            println(" */")
            println("${if (parentType != null) "override " else ""}val options: $compilerOptionType")
            deprecatedProperties
                .forEach {
                    println()
                    generatePropertyGetterAndSetter(it)
                }
        }
    }
}

private fun Printer.generateImpl(
    type: FqName,
    parentImplFqName: FqName?,
    parentType: FqName,
    properties: List<KotlinCompilerArgument>,
) {
    val modifiers = "internal abstract class"
    val afterType = if (parentImplFqName != null) {
        ": $parentImplFqName(objectFactory), $parentType"
    } else {
        ": $parentType"
    }
    generateDeclaration(
        modifiers,
        type,
        constructorDeclaration = "@javax.inject.Inject constructor(\n    objectFactory: org.gradle.api.model.ObjectFactory\n)",
        afterType = afterType
    ) {
        for (property in properties) {
            println()
            generatePropertyProviderImpl(property)
        }
    }
}

private fun Printer.generateCompilerOptionsHelper(
    type: FqName,
    helperName: FqName,
    parentHelperName: FqName?,
    argsType: FqName,
    properties: List<KotlinCompilerArgument>,
) {
    val modifiers = "internal object"

    generateDeclaration(
        modifiers,
        helperName,
    ) {
        println()
        println("internal fun fillCompilerArguments(")
        withIndent {
            println("from: $type,")
            println("args: $argsType,")
        }
        println(") {")
        withIndent {
            if (parentHelperName != null) println("$parentHelperName.fillCompilerArguments(from, args)")
            for (property in properties) {
                if (property.gradleDeprecatedOptionOrNull()?.level == DeprecationLevel.HIDDEN) continue

                val defaultValue = property.gradleValues
                if (property.name != "freeCompilerArgs") {
                    val getter = if (property.gradleReturnType.endsWith("?")) ".orNull" else ".get()"
                    val toArg = defaultValue.toArgumentConverter?.substringAfter("this") ?: ""
                    println("args.${property.calculateName()} = from.${property.gradleName}$getter$toArg")
                } else {
                    println("args.freeArgs += from.${property.gradleName}.get()")
                }
            }

            addAdditionalJvmArgs(helperName)
        }
        println("}")

        println()
        println("internal fun syncOptionsAsConvention(")
        withIndent {
            println("from: $type,")
            println("into: $type,")
        }
        println(") {")
        withIndent {
            val multiValuesReturnTypes = setOf(
                "org.gradle.api.provider.ListProperty",
                "org.gradle.api.provider.SetProperty",
            )
            if (parentHelperName != null) println("$parentHelperName.syncOptionsAsConvention(from, into)")
            for (property in properties) {
                if (property.gradleDeprecatedOptionOrNull()?.level == DeprecationLevel.HIDDEN) continue

                // Behaviour of ListProperty, SetProperty, MapProperty append operators in regard to convention value
                // is confusing for users: https://github.com/gradle/gradle/issues/18352
                // To make it less confusing for such types instead of wiring them via ".convention()" we updating
                // current value
                val gradleLazyReturnType = property.gradleLazyReturnType
                val mapper = when {
                    multiValuesReturnTypes.any { gradleLazyReturnType.startsWith(it) } -> "addAll"
                    gradleLazyReturnType.startsWith("org.gradle.api.provider.MapProperty") -> "putAll"
                    else -> "convention"
                }
                println("into.${property.gradleName}.$mapper(from.${property.gradleName})")
            }
        }
        println("}")
    }
}

private fun Printer.addAdditionalJvmArgs(implType: FqName) {
    // Adding required 'noStdlib' and 'noReflect' compiler arguments for JVM compilation
    // Otherwise compilation via build tools will fail
    if (implType.shortName().toString() == "KotlinJvmCompilerOptions$IMPLEMENTATION_HELPERS_SUFFIX") {
        println()
        println("// Arguments with always default values when used from build tools")
        println("args.noStdlib = true")
        println("args.noReflect = true")
        println("args.allowNoSourceFiles = true")
    }
}

internal fun Printer.generateDeclaration(
    modifiers: String,
    type: FqName,
    constructorDeclaration: String? = null,
    declarationKDoc: String? = null,
    afterType: String? = null,
    generateBody: Printer.() -> Unit,
) {
    println(
        """
        // DO NOT EDIT MANUALLY!
        // Generated by org/jetbrains/kotlin/generators/arguments/GenerateGradleOptions.kt
        // To regenerate run 'generateGradleOptions' task
        @file:Suppress("RemoveRedundantQualifierName", "Deprecation", "Deprecation_Error", "DuplicatedCode")
        
        """.trimIndent()
    )

    if (!type.parent().isRoot) {
        println("package ${type.parent()}")
        println()
    }

    if (declarationKDoc != null) {
        println("/**")
        declarationKDoc.split('\n').forEach {
            println(" * $it")
        }
        println(" */")
    }
    print("$modifiers ${type.shortName()}")
    constructorDeclaration?.let { print(" $it ") }
    afterType?.let { print("$afterType") }
    println(" {")
    withIndent {
        generateBody()
    }
    println("}")
}

private fun Printer.generatePropertyProvider(
    property: KotlinCompilerArgument,
    modifiers: String = "",
) {
    if (property.gradleDefaultValue == "null" &&
        property.gradleInputTypeAsEnum == GradleInputTypes.INPUT
    ) {
        println("@get:org.gradle.api.tasks.Optional")
    }
    println("@get:${property.gradleInputType}")
    println("${modifiers.appendWhitespaceIfNotBlank}val ${property.gradleName}: ${property.gradleLazyReturnType}")
}

private fun Printer.generatePropertyProviderImpl(
    property: KotlinCompilerArgument,
    modifiers: String = "",
) {
    generateOptionDeprecation(property)
    println(
        "override ${modifiers.appendWhitespaceIfNotBlank}val ${property.gradleName}: ${property.gradleLazyReturnType} ="
    )
    withIndent {
        val convention = if (property.gradleDefaultValue != "null") {
            ".convention(${property.gradleDefaultValue})"
        } else {
            ""
        }

        println(
            "objectFactory${property.gradleLazyReturnTypeInstantiator}$convention"
        )
    }
}

private fun Printer.generatePropertyGetterAndSetter(
    property: KotlinCompilerArgument,
    modifiers: String = "",
) {
    val defaultValue = property.gradleValues
    val returnType = property.gradleReturnType

    if (defaultValue.type != defaultValue.kotlinOptionsType) {
        assert(defaultValue.fromKotlinOptionConverterProp != null)
        assert(defaultValue.toKotlinOptionConverterProp != null)
    }

    if (defaultValue.fromKotlinOptionConverterProp != null) {
        println("private val ${defaultValue.kotlinOptionsType}.${property.gradleName}CompilerOption get() = ${defaultValue.fromKotlinOptionConverterProp}")
        println()
        println("private val ${defaultValue.type}.${property.gradleName}KotlinOption get() = ${defaultValue.toKotlinOptionConverterProp}")
        println()
    }

    val deprecationAnnotation = property.gradleDeprecatedOptionOrNull()

    generateDoc(property)
    if (deprecationAnnotation != null && deprecationAnnotation.level == DeprecationLevel.ERROR) {
        println(DeprecatedOptionAnnotator.generateOptionAnnotation(deprecationAnnotation))
    } else {
        println(
            kotlinOptionDeprecation(
                indent = 4,
                indentFirstLine = false,
                deprecationLevel = DeprecationLevel.WARNING,
            )
        )
    }
    println("${modifiers.appendWhitespaceIfNotBlank}var ${property.gradleName}: $returnType")
    val propGetter = if (returnType.endsWith("?")) ".orNull" else ".get()"
    val getter = if (defaultValue.fromKotlinOptionConverterProp != null) {
        "$propGetter.${property.gradleName}KotlinOption"
    } else {
        propGetter
    }
    val setter = if (defaultValue.toKotlinOptionConverterProp != null) {
        ".set(value.${property.gradleName}CompilerOption)"
    } else {
        ".set(value)"
    }
    withIndent {
        println("get() = options.${property.gradleName}$getter")
        println("set(value) = options.${property.gradleName}$setter")
    }
}

private fun KotlinCompilerArgument.gradleDeprecatedOptionOrNull(): GradleDeprecatedOption? =
    findMetadata<GradleDeprecatedOption>()

private val String.appendWhitespaceIfNotBlank get() = if (isNotBlank()) "$this " else ""

private fun Printer.generateOptionDeprecation(property: KotlinCompilerArgument) {
    property.gradleDeprecatedOptionOrNull()
        ?.let { DeprecatedOptionAnnotator.generateOptionAnnotation(it) }
        ?.also { println(it) }
}

private fun Printer.generateDoc(property: KotlinCompilerArgument) {
    val description = property.description.current.improveSpecificKdocRendering()
    val possibleValues = property.gradleValues.possibleValues
    val defaultValue = property.gradleValues.defaultValue

    println("/**")
    println(" * ${description.replace("\n", "\n$currentIndent * ")}")
    if (possibleValues != null) {
        println(" *")
        println(" * Possible values: ${possibleValues.joinToString()}")
    }
    println(" *")
    println(" * Default value: ${defaultValue.removePrefix("$OPTIONS_PACKAGE_PREFIX.")}")
    println(" */")
}

private fun String.improveSpecificKdocRendering(): String =
    // Render -jvm-default argument values as a list.
    replace("\n-jvm-default=([a-z-]*)".toRegex(), "\n* `-jvm-default=$1`")

internal inline fun Printer.withIndent(fn: Printer.() -> Unit) {
    pushIndent()
    fn()
    popIndent()
}

private fun generateMarkdown(properties: List<KotlinCompilerArgument>) {
    println("| Name | Description | Possible values |Default value |")
    println("|------|-------------|-----------------|--------------|")
    for (property in properties) {
        val name = property.gradleName
        if (name == "includeRuntime") continue   // This option has no effect in Gradle builds
        val renderName = listOfNotNull("`$name`", property.gradleDeprecatedOptionOrNull()?.let { "__(Deprecated)__" })
            .joinToString(" ")
        val description = property.description.current
        val possibleValues = property.gradleValues.possibleValues
        val defaultValue = when (property.gradleDefaultValue) {
            "null" -> ""
            "emptyList()" -> "[]"
            else -> property.gradleDefaultValue
        }

        println("| $renderName | $description | ${possibleValues.orEmpty().joinToString()} | $defaultValue |")
    }
}

private val KotlinCompilerArgument.gradleValues: DefaultValues
    get() = findMetadata<GradleOption>()!!.value.run {
        when (this) {
            DefaultValue.BOOLEAN_FALSE_DEFAULT -> DefaultValues.BooleanFalseDefault
            DefaultValue.BOOLEAN_TRUE_DEFAULT -> DefaultValues.BooleanTrueDefault
            DefaultValue.BOOLEAN_NULL_DEFAULT -> DefaultValues.BooleanNullDefault
            DefaultValue.STRING_NULL_DEFAULT -> DefaultValues.StringNullDefault
            DefaultValue.EMPTY_STRING_LIST_DEFAULT -> DefaultValues.EmptyStringListDefault
            DefaultValue.EMPTY_STRING_ARRAY_DEFAULT -> DefaultValues.EmptyStringArrayDefault
            DefaultValue.JVM_TARGET_VERSIONS -> DefaultValues.JvmTargetVersions
            DefaultValue.JVM_DEFAULT_MODES -> DefaultValues.JvmDefaultModes
            DefaultValue.LANGUAGE_VERSIONS -> DefaultValues.LanguageVersions
            DefaultValue.API_VERSIONS -> DefaultValues.ApiVersions
            DefaultValue.JS_MAIN -> DefaultValues.JsMain
            DefaultValue.JS_ECMA_VERSIONS -> DefaultValues.JsEcmaVersions
            DefaultValue.JS_MODULE_KINDS -> DefaultValues.JsModuleKinds
            DefaultValue.JS_SOURCE_MAP_CONTENT_MODES -> DefaultValues.JsSourceMapContentModes
            DefaultValue.JS_SOURCE_MAP_NAMES_POLICY -> DefaultValues.JsSourceMapNamesPolicies
        }
    }

private val KotlinCompilerArgument.gradleDefaultValue: String
    get() = gradleValues.defaultValue

private val KotlinCompilerArgument.gradleReturnType: String
    get() {
        // Set nullability based on Gradle default value
        var type = when (valueType::class
            .supertypes.single { it.classifier == KotlinArgumentValueType::class }
            .arguments.first().type!!.classifier as KClass<*>) {
            Boolean::class -> "kotlin.Boolean"
            Array<String>::class -> "kotlin.collections.List<kotlin.String>"
            else -> "kotlin.String"
        }

        if (gradleDefaultValue == "null") {
            type += "?"
        }
        return type
    }

private val KotlinCompilerArgument.gradleLazyReturnType: String
    get() {
        val returnType = gradleValues.type
        val classifier = returnType.classifier
        return when {
            classifier is KClass<*> && classifier == List::class ->
                "org.gradle.api.provider.ListProperty<${returnType.arguments.first().type!!.withNullability(false)}>"
            classifier is KClass<*> && classifier == Set::class ->
                "org.gradle.api.provider.SetProperty<${returnType.arguments.first().type!!.withNullability(false)}>"
            classifier is KClass<*> && classifier == Map::class ->
                "org.gradle.api.provider.MapProperty<${returnType.arguments[0]}, ${returnType.arguments[1]}"
            else -> "org.gradle.api.provider.Property<${returnType.withNullability(false)}>"
        }
    }

private val KotlinCompilerArgument.gradleLazyReturnTypeInstantiator: String
    get() {
        val returnType = gradleValues.type
        val classifier = returnType.classifier
        return when {
            classifier is KClass<*> && classifier == List::class ->
                ".listProperty(${returnType.arguments.first().type!!.withNullability(false)}::class.java)"
            classifier is KClass<*> && classifier == Set::class ->
                ".setProperty(${returnType.arguments.first().type!!.withNullability(false)}::class.java)"
            classifier is KClass<*> && classifier == Map::class ->
                ".mapProperty(${returnType.arguments[0]}::class.java, ${returnType.arguments[1]}::class.java)"
            else -> ".property(${returnType.withNullability(false)}::class.java)"
        }
    }

private val KotlinCompilerArgument.gradleName: String
    get() = findMetadata<GradleOption>()!!.gradleName.ifEmpty { calculateName() }

private val KotlinCompilerArgument.gradleInputTypeAsEnum: GradleInputTypes
    get() = findMetadata<GradleOption>()!!.gradleInputType

private val KotlinCompilerArgument.gradleInputType: String
    get() = findMetadata<GradleOption>()!!.gradleInputType.gradleType

private val KotlinCompilerArgument.generateDeprecatedKotlinOption: Boolean
    get() = findMetadata<GradleOption>()!!.shouldGenerateDeprecatedKotlinOptions

private inline fun <reified T> KotlinCompilerArgument.findMetadata(): T? =
    additionalMetadata.firstIsInstanceOrNull()

object DeprecatedOptionAnnotator {
    fun generateOptionAnnotation(annotation: GradleDeprecatedOption): String {
        val message = annotation.message.takeIf { it.isNotEmpty() }?.let { "message = \"$it\"" }
        val level = "level = DeprecationLevel.${annotation.level.name}"
        val arguments = listOfNotNull(message, level).joinToString()
        return "@Deprecated($arguments)"
    }
}
