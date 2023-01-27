/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.arguments

import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.File
import java.io.PrintStream
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.withNullability

// Additional properties that should be included in interface
@Suppress("unused")
interface AdditionalGradleProperties {
    @GradleOption(
        value = DefaultValue.EMPTY_STRING_LIST_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(value = "", description = "A list of additional compiler arguments")
    var freeCompilerArgs: List<String>
}

private data class GeneratedOptions(
    val optionsName: FqName,
    val deprecatedOptionsName: FqName,
    val properties: List<KProperty1<*, *>>
)

private const val GRADLE_API_SRC_DIR = "libraries/tools/kotlin-gradle-plugin-api/src/common/kotlin"
private const val GRADLE_PLUGIN_SRC_DIR = "libraries/tools/kotlin-gradle-plugin/src/common/kotlin"
private const val OPTIONS_PACKAGE_PREFIX = "org.jetbrains.kotlin.gradle.dsl"
private const val IMPLEMENTATION_SUFFIX = "Default"

fun generateKotlinGradleOptions(withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit) {
    val apiSrcDir = File(GRADLE_API_SRC_DIR)
    val srcDir = File(GRADLE_PLUGIN_SRC_DIR)

    val commonToolOptions = generateKotlinCommonToolOptions(apiSrcDir, withPrinterToFile)
    val commonToolImplFqName = generateKotlinCommonToolOptionsImpl(
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
    val commonCompilerOptionsImplFqName = generateKotlinCommonOptionsImpl(
        srcDir,
        commonCompilerOptions.optionsName,
        commonToolImplFqName,
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
        commonCompilerOptionsImplFqName,
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
        commonCompilerOptionsImplFqName,
        jsOptions.properties,
        withPrinterToFile
    )

    val jsDceOptions = generateJsDceOptions(
        apiSrcDir,
        commonToolOptions,
        withPrinterToFile
    )
    generateJsDceOptionsImpl(
        srcDir,
        jsDceOptions.optionsName,
        commonCompilerOptionsImplFqName,
        jsDceOptions.properties,
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
        commonCompilerOptionsImplFqName,
        multiplatformCommonOptions.properties,
        withPrinterToFile
    )
}

fun main() {
    fun getPrinter(file: File, fn: Printer.() -> Unit) {
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        PrintStream(file.outputStream()).use {
            val printer = Printer(it)
            printer.fn()
        }
    }

    generateKotlinGradleOptions(::getPrinter)
}

private fun generateKotlinCommonToolOptions(
    apiSrcDir: File,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit
): GeneratedOptions {
    val commonInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinCommonCompilerToolOptions")
    val commonOptions = gradleOptions<CommonToolArguments>()
    val additionalOptions = gradleOptions<AdditionalGradleProperties>()
    withPrinterToFile(fileFromFqName(apiSrcDir, commonInterfaceFqName)) {
        generateInterface(
            commonInterfaceFqName,
            commonOptions + additionalOptions
        )
    }

    val deprecatedCommonInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinCommonToolOptions")
    withPrinterToFile(fileFromFqName(apiSrcDir, deprecatedCommonInterfaceFqName)) {
        generateDeprecatedInterface(
            deprecatedCommonInterfaceFqName,
            commonInterfaceFqName,
            commonOptions + additionalOptions,
            parentType = null,
        )
    }

    println("### Attributes common for JVM, JS, and JS DCE\n")
    generateMarkdown(commonOptions + additionalOptions)

    return GeneratedOptions(commonInterfaceFqName, deprecatedCommonInterfaceFqName, (commonOptions + additionalOptions))
}

private fun generateKotlinCommonToolOptionsImpl(
    srcDir: File,
    commonToolOptionsInterfaceFqName: FqName,
    options: List<KProperty1<*, *>>,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit
): FqName {
    val k2CommonToolCompilerArgumentsFqName = FqName(CommonToolArguments::class.qualifiedName!!)
    val commonToolImplFqName = FqName("${commonToolOptionsInterfaceFqName.asString()}$IMPLEMENTATION_SUFFIX")
    withPrinterToFile(fileFromFqName(srcDir, commonToolImplFqName)) {
        generateImpl(
            commonToolImplFqName,
            null,
            commonToolOptionsInterfaceFqName,
            k2CommonToolCompilerArgumentsFqName,
            options,
        )
    }

    return commonToolImplFqName
}

private fun generateKotlinCommonOptions(
    apiSrcDir: File,
    commonToolGeneratedOptions: GeneratedOptions,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit
): GeneratedOptions {
    val commonCompilerInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinCommonCompilerOptions")
    val commonCompilerOptions = gradleOptions<CommonCompilerArguments>()
    withPrinterToFile(fileFromFqName(apiSrcDir, commonCompilerInterfaceFqName)) {
        generateInterface(
            commonCompilerInterfaceFqName,
            commonCompilerOptions,
            parentType = commonToolGeneratedOptions.optionsName,
        )
    }

    val deprecatedCommonCompilerInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinCommonOptions")
    withPrinterToFile(fileFromFqName(apiSrcDir, deprecatedCommonCompilerInterfaceFqName)) {
        generateDeprecatedInterface(
            deprecatedCommonCompilerInterfaceFqName,
            commonCompilerInterfaceFqName,
            commonCompilerOptions,
            parentType = commonToolGeneratedOptions.deprecatedOptionsName
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
    options: List<KProperty1<*, *>>,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit
): FqName {
    val k2CommonCompilerArgumentsFqName = FqName(CommonCompilerArguments::class.qualifiedName!!)
    val commonCompilerImplFqName = FqName("${commonOptionsInterfaceFqName.asString()}$IMPLEMENTATION_SUFFIX")
    withPrinterToFile(fileFromFqName(srcDir, commonCompilerImplFqName)) {
        generateImpl(
            commonCompilerImplFqName,
            commonToolImpl,
            commonOptionsInterfaceFqName,
            k2CommonCompilerArgumentsFqName,
            options,
        )
    }

    return commonCompilerImplFqName
}

private fun generateKotlinJvmOptions(
    apiSrcDir: File,
    commonCompilerGeneratedOptions: GeneratedOptions,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit
): GeneratedOptions {
    val jvmInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinJvmCompilerOptions")
    val jvmOptions = gradleOptions<K2JVMCompilerArguments>()
    withPrinterToFile(fileFromFqName(apiSrcDir, jvmInterfaceFqName)) {
        generateInterface(
            jvmInterfaceFqName,
            jvmOptions,
            parentType = commonCompilerGeneratedOptions.optionsName,
        )
    }

    val deprecatedJvmInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinJvmOptions")
    withPrinterToFile(fileFromFqName(apiSrcDir, deprecatedJvmInterfaceFqName)) {
        generateDeprecatedInterface(
            deprecatedJvmInterfaceFqName,
            jvmInterfaceFqName,
            jvmOptions,
            parentType = commonCompilerGeneratedOptions.deprecatedOptionsName
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
    jvmOptions: List<KProperty1<*, *>>,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit
) {
    val k2JvmCompilerArgumentsFqName = FqName(K2JVMCompilerArguments::class.qualifiedName!!)
    val jvmImplFqName = FqName("${jvmInterfaceFqName.asString()}$IMPLEMENTATION_SUFFIX")
    withPrinterToFile(fileFromFqName(srcDir, jvmImplFqName)) {
        generateImpl(
            jvmImplFqName,
            commonCompilerImpl,
            jvmInterfaceFqName,
            k2JvmCompilerArgumentsFqName,
            jvmOptions
        )
    }
}

private fun generateKotlinJsOptions(
    apiSrcDir: File,
    commonCompilerOptions: GeneratedOptions,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit
): GeneratedOptions {
    val jsInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinJsCompilerOptions")
    val jsOptions = gradleOptions<K2JSCompilerArguments>()
    withPrinterToFile(fileFromFqName(apiSrcDir, jsInterfaceFqName)) {
        generateInterface(
            jsInterfaceFqName,
            jsOptions,
            parentType = commonCompilerOptions.optionsName,
        )
    }

    val deprecatedJsInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinJsOptions")
    withPrinterToFile(fileFromFqName(apiSrcDir, deprecatedJsInterfaceFqName)) {
        generateDeprecatedInterface(
            deprecatedJsInterfaceFqName,
            jsInterfaceFqName,
            jsOptions,
            parentType = commonCompilerOptions.deprecatedOptionsName,
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
    jsOptions: List<KProperty1<*, *>>,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit
) {
    val k2JsCompilerArgumentsFqName = FqName(K2JSCompilerArguments::class.qualifiedName!!)
    val jsImplFqName = FqName("${jsInterfaceFqName.asString()}$IMPLEMENTATION_SUFFIX")
    withPrinterToFile(fileFromFqName(srcDir, jsImplFqName)) {
        generateImpl(
            jsImplFqName,
            commonCompilerImpl,
            jsInterfaceFqName,
            k2JsCompilerArgumentsFqName,
            jsOptions
        )
    }
}

private fun generateJsDceOptions(
    apiSrcDir: File,
    commonToolOptions: GeneratedOptions,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit
): GeneratedOptions {
    val jsDceInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinJsDceCompilerToolOptions")
    val jsDceOptions = gradleOptions<K2JSDceArguments>()
    withPrinterToFile(fileFromFqName(apiSrcDir, jsDceInterfaceFqName)) {
        generateInterface(
            jsDceInterfaceFqName,
            jsDceOptions,
            parentType = commonToolOptions.optionsName,
        )
    }

    val deprecatedJsDceInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinJsDceOptions")
    withPrinterToFile(fileFromFqName(apiSrcDir, deprecatedJsDceInterfaceFqName)) {
        generateDeprecatedInterface(
            deprecatedJsDceInterfaceFqName,
            jsDceInterfaceFqName,
            jsDceOptions,
            parentType = commonToolOptions.deprecatedOptionsName,
        )
    }

    println("\n### Attributes specific for JS/DCE\n")
    generateMarkdown(jsDceOptions)

    return GeneratedOptions(jsDceInterfaceFqName, deprecatedJsDceInterfaceFqName, jsDceOptions)
}

private fun generateJsDceOptionsImpl(
    srcDir: File,
    jsDceInterfaceFqName: FqName,
    commonCompilerImpl: FqName,
    jsDceOptions: List<KProperty1<*, *>>,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit
) {
    val k2JsDceArgumentsFqName = FqName(K2JSDceArguments::class.qualifiedName!!)
    val jsDceImplFqName = FqName("${jsDceInterfaceFqName.asString()}$IMPLEMENTATION_SUFFIX")
    withPrinterToFile(fileFromFqName(srcDir, jsDceImplFqName)) {
        generateImpl(
            jsDceImplFqName,
            commonCompilerImpl,
            jsDceInterfaceFqName,
            k2JsDceArgumentsFqName,
            jsDceOptions
        )
    }
}

private fun generateMultiplatformCommonOptions(
    apiSrcDir: File,
    commonCompilerOptions: GeneratedOptions,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit
): GeneratedOptions {
    val multiplatformCommonInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinMultiplatformCommonCompilerOptions")
    val multiplatformCommonOptions = gradleOptions<K2MetadataCompilerArguments>()
    withPrinterToFile(fileFromFqName(apiSrcDir, multiplatformCommonInterfaceFqName)) {
        generateInterface(
            multiplatformCommonInterfaceFqName,
            multiplatformCommonOptions,
            parentType = commonCompilerOptions.optionsName,
        )
    }

    val deprecatedMultiplatformCommonInterfaceFqName = FqName("$OPTIONS_PACKAGE_PREFIX.KotlinMultiplatformCommonOptions")
    withPrinterToFile(fileFromFqName(apiSrcDir, deprecatedMultiplatformCommonInterfaceFqName)) {
        generateDeprecatedInterface(
            deprecatedMultiplatformCommonInterfaceFqName,
            multiplatformCommonInterfaceFqName,
            parentType = commonCompilerOptions.deprecatedOptionsName,
            properties = multiplatformCommonOptions
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
    multiplatformCommonOptions: List<KProperty1<*, *>>,
    withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit
) {
    val k2metadataCompilerArgumentsFqName = FqName(K2MetadataCompilerArguments::class.qualifiedName!!)
    val multiplatformCommonImplFqName = FqName("${multiplatformCommonInterfaceFqName.asString()}$IMPLEMENTATION_SUFFIX")
    withPrinterToFile(fileFromFqName(srcDir, multiplatformCommonImplFqName)) {
        generateImpl(
            multiplatformCommonImplFqName,
            commonCompilerImpl,
            multiplatformCommonInterfaceFqName,
            k2metadataCompilerArgumentsFqName,
            multiplatformCommonOptions
        )
    }
}

private inline fun <reified T : Any> List<KProperty1<T, *>>.filterToBeDeleted() = filter { prop ->
    prop.findAnnotation<GradleDeprecatedOption>()
        ?.let { LanguageVersion.fromVersionString(it.removeAfter) }
        ?.let { it >= LanguageVersion.LATEST_STABLE }
        ?: true
}

private inline fun <reified T : Any> gradleOptions(): List<KProperty1<T, *>> =
    T::class
        .declaredMemberProperties
        .filter {
            it.findAnnotation<GradleOption>() != null
        }
        .filterToBeDeleted()
        .sortedBy { it.name }

internal fun fileFromFqName(baseDir: File, fqName: FqName): File {
    val fileRelativePath = fqName.asString().replace(".", "/") + ".kt"
    return File(baseDir, fileRelativePath)
}

private fun Printer.generateInterface(
    type: FqName,
    properties: List<KProperty1<*, *>>,
    parentType: FqName? = null,
) {
    val afterType = parentType?.let { " : $it" }
    generateDeclaration("interface", type, afterType = afterType) {
        for (property in properties) {
            println()
            generateDoc(property)
            generateOptionDeprecation(property)
            generatePropertyProvider(property)
        }
    }
}

private fun Printer.generateDeprecatedInterface(
    type: FqName,
    compilerOptionType: FqName,
    properties: List<KProperty1<*, *>>,
    parentType: FqName? = null,
) {
    val afterType = parentType?.let { " : $it" }
    // Add @Deprecated annotation back once proper migration to compilerOptions will be supported
    val modifier = """
    interface
    """.trimIndent()
    val deprecatedProperties = properties.filter { it.generateDeprecatedKotlinOption }
    // KotlinMultiplatformCommonOptions doesn't have any options, but it is being kept for backward compatibility
    if (deprecatedProperties.isNotEmpty() || type.asString().endsWith("KotlinMultiplatformCommonOptions")) {
        generateDeclaration(modifier, type, afterType = afterType) {

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
    argsType: FqName,
    properties: List<KProperty1<*, *>>
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

        println()
        println("internal fun fillCompilerArguments(args: $argsType) {")
        withIndent {
            if (parentImplFqName != null) println("super.fillCompilerArguments(args)")
            for (property in properties) {
                val defaultValue = property.gradleValues
                if (property.name != "freeCompilerArgs") {
                    val getter = if (property.gradleReturnType.endsWith("?")) ".orNull" else ".get()"
                    val toArg = defaultValue.toArgumentConverter?.substringAfter("this") ?: ""
                    println("args.${property.name} = ${property.name}$getter$toArg")
                } else {
                    println("args.freeArgs += ${property.name}.get()")
                }
            }

            addAdditionalJvmArgs(type)
        }
        println("}")

        println()
        println("internal fun fillDefaultValues(args: $argsType) {")
        withIndent {
            if (parentImplFqName != null) println("super.fillDefaultValues(args)")
            properties
                .filter { it.name != "freeCompilerArgs" }
                .forEach {
                    val defaultValue = it.gradleValues
                    var value = defaultValue.defaultValue
                    if (value != "null" && defaultValue.toArgumentConverter != null) {
                        value = "$value${defaultValue.toArgumentConverter.substringAfter("this")}"
                    }
                    println("args.${it.name} = $value")
                }

            addAdditionalJvmArgs(type)
        }
        println("}")
    }
}

private fun Printer.addAdditionalJvmArgs(implType: FqName) {
    // Adding required 'noStdlib' and 'noReflect' compiler arguments for JVM compilation
    // Otherwise compilation via build tools will fail
    if (implType.shortName().toString() == "KotlinJvmCompilerOptions$IMPLEMENTATION_SUFFIX") {
        println()
        println("// Arguments with always default values when used from build tools")
        println("args.noStdlib = true")
        println("args.noReflect = true")
    }
}

internal fun Printer.generateDeclaration(
    modifiers: String,
    type: FqName,
    constructorDeclaration: String? = null,
    afterType: String? = null,
    generateBody: Printer.() -> Unit
) {
    println(
        """
        // DO NOT EDIT MANUALLY!
        // Generated by org/jetbrains/kotlin/generators/arguments/GenerateGradleOptions.kt
        // To regenerate run 'generateGradleOptions' task
        @file:Suppress("RemoveRedundantQualifierName", "Deprecation", "DuplicatedCode")
        
        """.trimIndent()
    )

    if (!type.parent().isRoot) {
        println("package ${type.parent()}")
        println()
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
    property: KProperty1<*, *>,
    modifiers: String = ""
) {
    if (property.gradleDefaultValue == "null" &&
        property.gradleInputTypeAsEnum == GradleInputTypes.INPUT
    ) {
        println("@get:org.gradle.api.tasks.Optional")
    }
    println("@get:${property.gradleInputType}")
    println("${modifiers.appendWhitespaceIfNotBlank}val ${property.name}: ${property.gradleLazyReturnType}")
}

private fun Printer.generatePropertyProviderImpl(
    property: KProperty1<*, *>,
    modifiers: String = ""
) {
    generateOptionDeprecation(property)
    println(
        "override ${modifiers.appendWhitespaceIfNotBlank}val ${property.name}: ${property.gradleLazyReturnType} ="
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
    property: KProperty1<*, *>,
    modifiers: String = "",
) {
    val defaultValue = property.gradleValues
    val returnType = property.gradleReturnType

    if (defaultValue.type != defaultValue.kotlinOptionsType) {
        assert(defaultValue.fromKotlinOptionConverterProp != null)
        assert(defaultValue.toKotlinOptionConverterProp != null)
    }

    if (defaultValue.fromKotlinOptionConverterProp != null) {
        println("private val ${defaultValue.kotlinOptionsType}.${property.name}CompilerOption get() = ${defaultValue.fromKotlinOptionConverterProp}")
        println()
        println("private val ${defaultValue.type}.${property.name}KotlinOption get() = ${defaultValue.toKotlinOptionConverterProp}")
        println()
    }

    generateDoc(property)
    generateOptionDeprecation(property)
    println("${modifiers.appendWhitespaceIfNotBlank}var ${property.name}: $returnType")
    val propGetter = if (returnType.endsWith("?")) ".orNull" else ".get()"
    val getter = if (defaultValue.fromKotlinOptionConverterProp != null) {
        "$propGetter.${property.name}KotlinOption"
    } else {
        propGetter
    }
    val setter = if (defaultValue.toKotlinOptionConverterProp != null) {
        ".set(value.${property.name}CompilerOption)"
    } else {
        ".set(value)"
    }
    withIndent {
        println("get() = options.${property.name}$getter")
        println("set(value) = options.${property.name}$setter")
    }
}

private val String.appendWhitespaceIfNotBlank get() = if (isNotBlank()) "$this " else ""

private fun Printer.generateOptionDeprecation(property: KProperty1<*, *>) {
    property.findAnnotation<GradleDeprecatedOption>()
        ?.let { DeprecatedOptionAnnotator.generateOptionAnnotation(it) }
        ?.also { println(it) }
}

private fun Printer.generateDoc(property: KProperty1<*, *>) {
    val description = property.findAnnotation<Argument>()!!.description
    val possibleValues = property.gradleValues.possibleValues
    val defaultValue = property.gradleDefaultValue

    println("/**")
    println(" * $description")
    if (possibleValues != null) {
        println(" * Possible values: ${possibleValues.joinToString()}")
    }
    println(" * Default value: $defaultValue")
    println(" */")
}

internal inline fun Printer.withIndent(fn: Printer.() -> Unit) {
    pushIndent()
    fn()
    popIndent()
}

private fun generateMarkdown(properties: List<KProperty1<*, *>>) {
    println("| Name | Description | Possible values |Default value |")
    println("|------|-------------|-----------------|--------------|")
    for (property in properties) {
        val name = property.name
        if (name == "includeRuntime") continue   // This option has no effect in Gradle builds
        val renderName = listOfNotNull("`$name`", property.findAnnotation<GradleDeprecatedOption>()?.let { "__(Deprecated)__" })
            .joinToString(" ")
        val description = property.findAnnotation<Argument>()!!.description
        val possibleValues = property.gradleValues.possibleValues
        val defaultValue = when (property.gradleDefaultValue) {
            "null" -> ""
            "emptyList()" -> "[]"
            else -> property.gradleDefaultValue
        }

        println("| $renderName | $description | ${possibleValues.orEmpty().joinToString()} | $defaultValue |")
    }
}

private val KProperty1<*, *>.gradleValues: DefaultValues
    get() = findAnnotation<GradleOption>()!!.value.run {
        when (this) {
            DefaultValue.BOOLEAN_FALSE_DEFAULT -> DefaultValues.BooleanFalseDefault
            DefaultValue.BOOLEAN_TRUE_DEFAULT -> DefaultValues.BooleanTrueDefault
            DefaultValue.STRING_NULL_DEFAULT -> DefaultValues.StringNullDefault
            DefaultValue.EMPTY_STRING_LIST_DEFAULT -> DefaultValues.EmptyStringListDefault
            DefaultValue.JVM_TARGET_VERSIONS -> DefaultValues.JvmTargetVersions
            DefaultValue.LANGUAGE_VERSIONS -> DefaultValues.LanguageVersions
            DefaultValue.API_VERSIONS -> DefaultValues.ApiVersions
            DefaultValue.JS_MAIN -> DefaultValues.JsMain
            DefaultValue.JS_ECMA_VERSIONS -> DefaultValues.JsEcmaVersions
            DefaultValue.JS_MODULE_KINDS -> DefaultValues.JsModuleKinds
            DefaultValue.JS_SOURCE_MAP_CONTENT_MODES -> DefaultValues.JsSourceMapContentModes
            DefaultValue.JS_SOURCE_MAP_NAMES_POLICY -> DefaultValues.JsSourceMapNamesPolicies
        }
    }

private val KProperty1<*, *>.gradleDefaultValue: String
    get() = gradleValues.defaultValue

private val KProperty1<*, *>.gradleReturnType: String
    get() {
        // Set nullability based on Gradle default value
        var type = returnType.withNullability(false).toString().substringBeforeLast("!")
        if (gradleDefaultValue == "null") {
            type += "?"
        }
        return type
    }

private val KProperty1<*, *>.gradleLazyReturnType: String
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

private val KProperty1<*, *>.gradleLazyReturnTypeInstantiator: String
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

private val KProperty1<*, *>.gradleInputTypeAsEnum: GradleInputTypes
    get() = findAnnotation<GradleOption>()!!.gradleInputType

private val KProperty1<*, *>.gradleInputType: String
    get() = findAnnotation<GradleOption>()!!.gradleInputType.gradleType

private val KProperty1<*, *>.generateDeprecatedKotlinOption: Boolean
    get() = findAnnotation<GradleOption>()!!.shouldGenerateDeprecatedKotlinOptions

private inline fun <reified T> KAnnotatedElement.findAnnotation(): T? =
    annotations.firstIsInstanceOrNull()

object DeprecatedOptionAnnotator {
    fun generateOptionAnnotation(annotation: GradleDeprecatedOption): String {
        val message = annotation.message.takeIf { it.isNotEmpty() }?.let { "message = \"$it\"" }
        val level = "level = DeprecationLevel.${annotation.level.name}"
        val arguments = listOfNotNull(message, level).joinToString()
        return "@Deprecated($arguments)"
    }
}
