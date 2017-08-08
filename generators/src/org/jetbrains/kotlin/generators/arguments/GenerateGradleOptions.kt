/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.generators.arguments

import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer
import java.io.File
import java.io.PrintStream
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KProperty1
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

fun generateKotlinGradleOptions(withPrinterToFile: (targetFile: File, Printer.()->Unit)->Unit) {
    val srcDir = File("libraries/tools/kotlin-gradle-plugin/src/main/kotlin")

    // common interface
    val commonInterfaceFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinCommonToolOptions")
    val commonOptions =  gradleOptions<CommonToolArguments>()
    val additionalOptions = gradleOptions<AdditionalGradleProperties>()
    withPrinterToFile(File(srcDir, commonInterfaceFqName)) {
        generateInterface(commonInterfaceFqName,
                          commonOptions + additionalOptions)
    }

    println("### Attributes common for 'kotlin', 'kotlin2js' and 'kotlin2jsDce\n")
    generateMarkdown(commonOptions + additionalOptions)

    val commonCompilerInterfaceFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions")
    val commonCompilerOptions = gradleOptions<CommonCompilerArguments>()
    withPrinterToFile(File(srcDir, commonCompilerInterfaceFqName)) {
        generateInterface(commonCompilerInterfaceFqName,
                          commonCompilerOptions,
                          parentType = commonInterfaceFqName)
    }

    println("\n### Attributes common for 'kotlin' and 'kotlin2js'\n")
    generateMarkdown(commonCompilerOptions)

    // generate jvm interface
    val jvmInterfaceFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions")
    val jvmOptions = gradleOptions<K2JVMCompilerArguments>()
    withPrinterToFile(File(srcDir, jvmInterfaceFqName)) {
        generateInterface(jvmInterfaceFqName,
                          jvmOptions,
                          parentType = commonCompilerInterfaceFqName)
    }

    // generate jvm impl
    val k2JvmCompilerArgumentsFqName = FqName(K2JVMCompilerArguments::class.qualifiedName!!)
    val jvmImplFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsBase")
    withPrinterToFile(File(srcDir, jvmImplFqName)) {
        generateImpl(jvmImplFqName,
                     jvmInterfaceFqName,
                     k2JvmCompilerArgumentsFqName,
                     commonOptions + commonCompilerOptions + jvmOptions)
    }

    println("\n### Attributes specific for 'kotlin'\n")
    generateMarkdown(jvmOptions)

    // generate js interface
    val jsInterfaceFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions")
    val jsOptions = gradleOptions<K2JSCompilerArguments>()
    withPrinterToFile(File(srcDir, jsInterfaceFqName)) {
        generateInterface(jsInterfaceFqName,
                          jsOptions,
                          parentType = commonCompilerInterfaceFqName)
    }

    val k2JsCompilerArgumentsFqName = FqName(K2JSCompilerArguments::class.qualifiedName!!)
    val jsImplFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinJsOptionsBase")
    withPrinterToFile(File(srcDir, jsImplFqName)) {
        generateImpl(jsImplFqName,
                     jsInterfaceFqName,
                     k2JsCompilerArgumentsFqName,
                     commonOptions + commonCompilerOptions + jsOptions)
    }

    println("\n### Attributes specific for 'kotlin2js'\n")
    generateMarkdown(jsOptions)

    // generate JS DCE interface and implementation
    val jsDceInterfaceFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinJsDceOptions")
    val jsDceOptions = gradleOptions<K2JSDceArguments>()
    withPrinterToFile(File(srcDir, jsDceInterfaceFqName)) {
        generateInterface(jsDceInterfaceFqName,
                          jsDceOptions,
                          parentType = commonInterfaceFqName)
    }

    val k2JsDceArgumentsFqName = FqName(K2JSDceArguments::class.qualifiedName!!)
    val jsDceImplFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinJsDceOptionsBase")
    withPrinterToFile(File(srcDir, jsDceImplFqName)) {
        generateImpl(jsDceImplFqName,
                     jsDceInterfaceFqName,
                     k2JsDceArgumentsFqName,
                     commonOptions + jsDceOptions)
    }
}

fun main(args: Array<String>) {
    fun getPrinter(file: File, fn: Printer.()->Unit) {
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

private inline fun <reified T : Any> gradleOptions(): List<KProperty1<T, *>> =
        T::class.declaredMemberProperties.filter { it.findAnnotation<GradleOption>() != null }.sortedBy { it.name }

private fun File(baseDir: File, fqName: FqName): File {
    val fileRelativePath = fqName.asString().replace(".", "/") + ".kt"
    return File(baseDir, fileRelativePath)
}

private fun Printer.generateInterface(type: FqName, properties: List<KProperty1<*, *>>, parentType: FqName? = null) {
    val afterType = parentType?.let { " : $it" }
    generateDeclaration("interface", type, afterType = afterType) {
        for (property in properties) {
            println()
            generateDoc(property)
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
            val backingField = property.backingField()
            val backingFieldType = property.gradleReturnType + "?"
            println("private var $backingField: $backingFieldType = null")
            generatePropertyDeclaration(property, modifiers = "override")
            withIndent {
                println("get() = $backingField ?: ${property.gradleDefaultValue}")
                println("set(value) { $backingField = value }")
            }
        }

        println()
        println("internal open fun updateArguments(args: $argsType) {")
        withIndent {
            for (property in properties) {
                val backingField = property.backingField()
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
    }
    println("}")
}

private fun Printer.generateDeclaration(
        modifiers: String,
        type: FqName,
        afterType: String? = null,
        generateBody: Printer.()->Unit
) {
    println("// DO NOT EDIT MANUALLY!")
    println("// Generated by org/jetbrains/kotlin/generators/arguments/GenerateGradleOptions.kt")
    if (!type.parent().isRoot) {
        println("package ${type.parent()}")
        println()
    }
    print("$modifiers ${type.shortName()} ")
    afterType?.let { print("$afterType ") }
    println("{")
    withIndent {
        generateBody()
    }
    println("}")
}

private fun Printer.generatePropertyDeclaration(property: KProperty1<*, *>, modifiers: String = "") {
    val returnType = property.gradleReturnType
    println("$modifiers var ${property.name}: $returnType")
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

private inline fun Printer.withIndent(fn: Printer.()->Unit) {
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
        val description = property.findAnnotation<Argument>()!!.description
        val possibleValues = property.gradleValues.possibleValues
        val defaultValue = when (property.gradleDefaultValue) {
            "null" -> ""
            "emptyList()" -> "[]"
            else -> property.gradleDefaultValue
        }

        println("| `$name` | $description | ${possibleValues.orEmpty().joinToString()} | $defaultValue |")
    }
}

private val KProperty1<*, *>.gradleValues: DefaultValues
        get() = findAnnotation<GradleOption>()!!.value.objectInstance!!

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

private inline fun <reified T> KAnnotatedElement.findAnnotation(): T? =
        annotations.filterIsInstance<T>().firstOrNull()
