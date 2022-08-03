/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.arguments

import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer
import java.io.File
import java.io.PrintStream
import java.util.*
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.withNullability

// Additional properties that should be included in interface
@Suppress("unused")
interface AdditionalGradleProperties {
    @GradleOption(EmptyList::class)
    @Argument(value = "", description = "A list of additional compiler arguments")
    var freeCompilerArgs: List<String>

    object EmptyList : DefaultValues("emptyList()")
}

fun generateKotlinGradleOptions(withPrinterToFile: (targetFile: File, Printer.() -> Unit) -> Unit) {
    val apiSrcDir = File("libraries/tools/kotlin-gradle-plugin-api/src/common/kotlin")
    val srcDir = File("libraries/tools/kotlin-gradle-plugin/src/common/kotlin")

    // specific Gradle types from internal compiler types
    generateKotlinVersion(apiSrcDir, withPrinterToFile)

    // common interface
    val commonInterfaceFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinCommonToolOptions")
    val commonOptions = gradleOptions<CommonToolArguments>()
    val additionalOptions = gradleOptions<AdditionalGradleProperties>()
    withPrinterToFile(file(apiSrcDir, commonInterfaceFqName)) {
        generateInterface(
            commonInterfaceFqName,
            commonOptions + additionalOptions
        )
    }

    println("### Attributes common for JVM, JS, and JS DCE\n")
    generateMarkdown(commonOptions + additionalOptions)

    val commonCompilerInterfaceFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions")
    val commonCompilerOptions = gradleOptions<CommonCompilerArguments>()
    withPrinterToFile(file(apiSrcDir, commonCompilerInterfaceFqName)) {
        generateInterface(
            commonCompilerInterfaceFqName,
            commonCompilerOptions,
            parentType = commonInterfaceFqName
        )
    }

    println("\n### Attributes common for JVM and JS\n")
    generateMarkdown(commonCompilerOptions)

    // generate jvm interface
    val jvmInterfaceFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions")
    val jvmOptions = gradleOptions<K2JVMCompilerArguments>()
    withPrinterToFile(file(apiSrcDir, jvmInterfaceFqName)) {
        generateInterface(
            jvmInterfaceFqName,
            jvmOptions,
            parentType = commonCompilerInterfaceFqName
        )
    }

    // generate jvm impl
    val k2JvmCompilerArgumentsFqName = FqName(K2JVMCompilerArguments::class.qualifiedName!!)
    val jvmImplFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsBase")
    withPrinterToFile(file(srcDir, jvmImplFqName)) {
        generateImpl(
            jvmImplFqName,
            jvmInterfaceFqName,
            k2JvmCompilerArgumentsFqName,
            commonOptions + commonCompilerOptions + jvmOptions
        )
    }

    println("\n### Attributes specific for JVM\n")
    generateMarkdown(jvmOptions)

    // generate js interface
    val jsInterfaceFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions")
    val jsOptions = gradleOptions<K2JSCompilerArguments>()
    withPrinterToFile(file(apiSrcDir, jsInterfaceFqName)) {
        generateInterface(
            jsInterfaceFqName,
            jsOptions,
            parentType = commonCompilerInterfaceFqName
        )
    }

    val k2JsCompilerArgumentsFqName = FqName(K2JSCompilerArguments::class.qualifiedName!!)
    val jsImplFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinJsOptionsBase")
    withPrinterToFile(file(srcDir, jsImplFqName)) {
        generateImpl(
            jsImplFqName,
            jsInterfaceFqName,
            k2JsCompilerArgumentsFqName,
            commonOptions + commonCompilerOptions + jsOptions
        )
    }

    println("\n### Attributes specific for JS\n")
    generateMarkdown(jsOptions)

    // generate JS DCE interface and implementation
    val jsDceInterfaceFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinJsDceOptions")
    val jsDceOptions = gradleOptions<K2JSDceArguments>()
    withPrinterToFile(file(apiSrcDir, jsDceInterfaceFqName)) {
        generateInterface(
            jsDceInterfaceFqName,
            jsDceOptions,
            parentType = commonInterfaceFqName
        )
    }

    val k2JsDceArgumentsFqName = FqName(K2JSDceArguments::class.qualifiedName!!)
    val jsDceImplFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinJsDceOptionsBase")
    withPrinterToFile(file(srcDir, jsDceImplFqName)) {
        generateImpl(
            jsDceImplFqName,
            jsDceInterfaceFqName,
            k2JsDceArgumentsFqName,
            commonOptions + jsDceOptions
        )
    }

    // generate multiplatform common interface and implementation
    val multiplatformCommonInterfaceFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonOptions")
    val multiplatformCommonOptions = gradleOptions<K2MetadataCompilerArguments>()
    withPrinterToFile(file(srcDir, multiplatformCommonInterfaceFqName)) {
        generateInterface(
            multiplatformCommonInterfaceFqName,
            multiplatformCommonOptions,
            parentType = commonCompilerInterfaceFqName
        )
    }

    val k2metadataCompilerArgumentsFqName = FqName(K2MetadataCompilerArguments::class.qualifiedName!!)
    val multiplatformCommonImplFqName = FqName(multiplatformCommonInterfaceFqName.asString() + "Base")
    withPrinterToFile(file(srcDir, multiplatformCommonImplFqName)) {
        generateImpl(
            multiplatformCommonImplFqName,
            multiplatformCommonInterfaceFqName,
            k2metadataCompilerArgumentsFqName,
            commonOptions + commonCompilerOptions + multiplatformCommonOptions
        )
    }

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

private inline fun <reified T : Any> List<KProperty1<T, *>>.filterToBeDeleted() = filter { prop ->
    prop.findAnnotation<GradleDeprecatedOption>()
        ?.let { LanguageVersion.fromVersionString(it.removeAfter) }
        ?.let { it >= LanguageVersion.LATEST_STABLE }
        ?: true
}

private inline fun <reified T : Any> gradleOptions(): List<KProperty1<T, *>> =
    T::class.declaredMemberProperties.filter { it.findAnnotation<GradleOption>() != null }.filterToBeDeleted().sortedBy { it.name }

internal fun file(baseDir: File, fqName: FqName): File {
    val fileRelativePath = fqName.asString().replace(".", "/") + ".kt"
    return File(baseDir, fileRelativePath)
}

private fun Printer.generateInterface(type: FqName, properties: List<KProperty1<*, *>>, parentType: FqName? = null) {
    val afterType = parentType?.let { " : $it" }
    generateDeclaration("interface", type, afterType = afterType) {
        for (property in properties) {
            println()
            generateDoc(property)
            generateOptionDeprecation(property)
            generatePropertyDeclaration(property)
        }
    }
}

private fun Printer.generateImpl(
    type: FqName,
    parentType: FqName,
    argsType: FqName,
    properties: List<KProperty1<*, *>>
) {
    generateDeclaration("internal abstract class", type, afterType = ": $parentType") {
        fun KProperty1<*, *>.backingField(): String = "${this.name}Field"

        for (property in properties) {
            println()
            val propertyType = property.gradleReturnType
            if (propertyType.endsWith("?")) {
                generateOptionDeprecation(property)
                generatePropertyDeclaration(property, modifiers = "override", value = "null")
            } else {
                val backingField = property.backingField()
                val visibilityModified = property.gradleBackingFieldVisibility.name.lowercase(Locale.US)
                println("$visibilityModified var $backingField: $propertyType? = null")
                generateOptionDeprecation(property)
                generatePropertyDeclaration(property, modifiers = "override")
                withIndent {
                    println("get() = $backingField ?: ${property.gradleDefaultValue}")
                    println("set(value) {")
                    withIndent { println("$backingField = value") }
                    println("}")
                }
            }
        }

        println()
        println("internal open fun updateArguments(args: $argsType) {")
        withIndent {
            for (property in properties) {
                val backingField = if (property.gradleReturnType.endsWith("?")) property.name else property.backingField()
                println("$backingField?.let { args.${property.name} = it }")
            }
        }
        println("}")
    }

    println()
    println("internal fun $argsType.fillDefaultValues() {")
    withIndent {
        for (property in properties) {
            println("${property.name} = ${property.gradleDefaultValue}")
        }
        // Adding required 'noStdlib' and 'noReflect' compiler arguments for JVM compilation
        // Otherwise compilation via build tools will fail
        if (type.shortName().toString() == "KotlinJvmOptionsBase") {
            println("noStdlib = true")
            println("noReflect = true")
        }
    }
    println("}")
}

internal fun Printer.generateDeclaration(
    modifiers: String,
    type: FqName,
    afterType: String? = null,
    generateBody: Printer.() -> Unit
) {
    println("// DO NOT EDIT MANUALLY!")
    println("// Generated by org/jetbrains/kotlin/generators/arguments/GenerateGradleOptions.kt")
    if (!type.parent().isRoot) {
        println("package ${type.parent()}")
        println()
    }
    println("@Suppress(\"DEPRECATION\")")
    print("$modifiers ${type.shortName()} ")
    afterType?.let { print("$afterType ") }
    println("{")
    withIndent {
        generateBody()
    }
    println("}")
}

private fun Printer.generatePropertyDeclaration(property: KProperty1<*, *>, modifiers: String = "", value: String? = null) {
    val returnType = property.gradleReturnType
    val initialValue = if (value != null) " = $value" else ""
    println("$modifiers var ${property.name}: $returnType$initialValue")
}

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
    get() = findAnnotation<GradleOption>()!!.value.objectInstance!!

private val KProperty1<*, *>.gradleDefaultValue: String
    get() = gradleValues.defaultValue

private val KProperty1<*, *>.gradleBackingFieldVisibility: KVisibility
    get() {
        val fieldVisibility = findAnnotation<GradleOption>()!!.backingFieldVisibility
        require(fieldVisibility != KVisibility.PUBLIC) {
            "Backing field should not have public visibility!"
        }
        return fieldVisibility
    }

private val KProperty1<*, *>.gradleReturnType: String
    get() {
        // Set nullability based on Gradle default value
        var type = returnType.withNullability(false).toString().substringBeforeLast("!")
        if (gradleDefaultValue == "null") {
            type += "?"
        }
        return type
    }

private inline fun <reified T> KAnnotatedElement.findAnnotation(): T? =
    annotations.filterIsInstance<T>().firstOrNull()

object DeprecatedOptionAnnotator {
    fun generateOptionAnnotation(annotation: GradleDeprecatedOption): String {
        val message = annotation.message.takeIf { it.isNotEmpty() }?.let { "message = \"$it\"" }
        val level = "level = DeprecationLevel.${annotation.level.name}"
        val arguments = listOfNotNull(message, level).joinToString()
        return "@Deprecated($arguments)"
    }
}