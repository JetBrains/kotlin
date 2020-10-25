/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.checkers.*
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.io.File
import java.lang.Exception

private data class ParsedMethod(
    val returnType: String,
    val funName: String,
    val typeParameters: String,
    val valueParameters: String,
    val body: String,
    val jspecifyMark: String? = null,
    val jspecifyAnnotation: String? = null
)

private data class ParsedInterface(
    val name: String,
    val superType: String,
    val methods: List<ParsedMethod>
)

private data class ParsedCallComponent(
    val callee: String,
    val typeArguments: String?,
    val valueArguments: String?
)

private data class ParsedCall(
    val jspecifyMark: String?,
    val receiver: String,
    val callComponents: Sequence<ParsedCallComponent>,
    val indent: String
)

class GenerateKotlinUseSitesFromJavaOnesForJspecifyTests {
    companion object {
        private const val TESTS_DIRECTORY = "compiler/testData/foreignAnnotationsJava8/tests/jspecify/java"

        private const val OVERRIDE_ANNOTATION = "@Override"
        private const val JSPECIFY_ANNOTATIONS = """(?:$JSPECIFY_NULLABLE_ANNOTATION|$JSPECIFY_NULLNESS_UNSPECIFIED_ANNOTATION)"""
        private const val JSPECIFY_MARK =
            """(?:\/\/ (?:$JSPECIFY_NULLNESS_MISMATCH_MARK|$JSPECIFY_NULLNESS_NOT_ENOUGH_INFORMATION_MARK))\n\s*"""
        private const val TYPE_COMPONENT = """(?:$JSPECIFY_ANNOTATIONS )?(?:\w+)(?:\.\w+)*"""
        private const val TYPE_ARGUMENT = """$TYPE_COMPONENT|(?:\? (?:extends|super) $TYPE_COMPONENT)|\?"""
        private const val TYPE_ARGUMENTS = """<(?:$TYPE_ARGUMENT)(?:\s*,\s*$TYPE_ARGUMENT)*>"""
        private const val TYPE_PARAMETER = """\w(?: (?:extends|super) $TYPE_COMPONENT)?"""
        private const val TYPE_PARAMETERS = """<(?:$TYPE_PARAMETER)(?:\s*,\s*$TYPE_PARAMETER)*>\s*"""
        private const val TYPE = """$TYPE_COMPONENT(?:$TYPE_ARGUMENTS)?"""
        private const val VALUE_PARAMETER = """\s*(?:$TYPE) (?:\w+)"""
        private const val VALUE_PARAMETERS = """\((?:(?:$VALUE_PARAMETER)(?:\s*,\s*$VALUE_PARAMETER)*\s*)?\)"""
        private const val VALUE_ARGUMENT = """\w+"""
        private const val VALUE_ARGUMENTS = """\((?:(?:$VALUE_ARGUMENT)(?:\s*,\s*$VALUE_ARGUMENT)*\s*)?\)"""
        private const val CALL_COMPONENT = """\.($TYPE_ARGUMENTS)?(?:\w+)(?:$VALUE_ARGUMENTS)?"""
        private const val CALL = """\s*(?:$JSPECIFY_MARK)?(?:\w+)(?:(?:$CALL_COMPONENT)+);(\n+)\s*"""
        private const val FUN_BODY = """\{((?:$CALL)+)\s*}"""

        val methodRegex = Regex("""(?:public )?($TYPE_PARAMETERS)?(void|$TYPE) (\w+)($VALUE_PARAMETERS)? $FUN_BODY""")
        val overrideMethodRegex =
            Regex("""$OVERRIDE_ANNOTATION\n\s*($JSPECIFY_ANNOTATIONS\n\s*)?($JSPECIFY_MARK)?(?:public )?($TYPE_PARAMETERS)?(void|$TYPE) (\w+)($VALUE_PARAMETERS);""")
        val interfaceWithOverrides = Regex("""(?:public )?interface (\w+) extends ($TYPE) \{\s*((?:$overrideMethodRegex\s*)+)}""")
        val topLevelInterface = Regex("""(?:^|\n)(?:($JSPECIFY_DEFAULT_NOT_NULL_ANNOTATION)\n)?(?:public )?(?:interface|class) (\w+)""")
        val valueParameterRegex = Regex("""($TYPE) (\w+)(,|\s*$)""")
        val callRegex = Regex(CALL.replace("?:", ""))
        val callComponentRegex = Regex(CALL_COMPONENT.replace("?:", ""))

        val generationNotNeededFiles = setOf("DereferenceClass.java")
    }

    private fun parseMethod(sourceCode: String): ParsedMethod? {
        val parsedResult = methodRegex.find(sourceCode) ?: return null
        val (typeParameters, returnType, funName, arguments, body) = parsedResult.destructured

        return ParsedMethod(
            transformTypesByAnnotationsIfNeeded(returnType),
            funName,
            typeParameters,
            arguments.removeSurrounding("(", ")"),
            body
        )
    }

    private fun parseCalls(funBody: String) =
        callRegex.findAll(funBody).map { call ->
            val jspecifyMark = call.groups[2]?.value
            val receiver = call.groups[4]?.value ?: throw Exception("Unable to parse receiver of $call")
            val callComponents = call.groups[5] ?: throw Exception("Unable to parse call components of $call")
            val indent = call.groups[35]?.value ?: throw Exception("Unable to parse indent of $call")
            val parsedCallComponents = callComponentRegex.findAll(callComponents.value).map { callComponent ->
                val calle = callComponent.groups[24]?.value
                    ?: throw Exception("Unable to parse calle of $callComponent")
                val typeArguments = callComponent.groups[1]?.value
                val valueArguments = callComponent.groups[25]?.value?.removeSurrounding("(", ")")
                    ?.let { transformTypesByAnnotationsIfNeeded(it) }
                ParsedCallComponent(calle, typeArguments, valueArguments)
            }
            ParsedCall(jspecifyMark, receiver, parsedCallComponents, indent)
        }

    private fun parseOverrides(sourceCode: String): Pair<List<ParsedInterface>, Pair<String, String>?>? {
        val topLevelClassMatch = topLevelInterface.find(sourceCode)?.groupValues
        val topLevelNullnessAnnotation = topLevelClassMatch?.get(1)
        val topLevelClass = topLevelClassMatch?.get(2)
        val parsedInterfaces = interfaceWithOverrides.findAll(sourceCode).map { interfacesMatch ->
            val (name, superType, body) = interfacesMatch.destructured
            val overrides = overrideMethodRegex.findAll(body).map {
                val (jspecifyAnnotation, jspecifyMark, typeParameters, returnType, methodName, valueParameters) = it.destructured
                ParsedMethod(
                    returnType, methodName, typeParameters, valueParameters.removeSurrounding("(", ")"),
                    body = "", jspecifyMark, jspecifyAnnotation
                )
            }.toList()

            ParsedInterface(name, superType, overrides)
        }.toList()
        val topLevelClassInfo =
            if (topLevelClass != null && topLevelNullnessAnnotation != null) topLevelClass to topLevelNullnessAnnotation else null

        return if (parsedInterfaces.isEmpty()) null else parsedInterfaces to topLevelClassInfo
    }

    private fun transformTypesByAnnotationsIfNeeded(containedTypes: String) =
        containedTypes
            .replace(Regex("""$JSPECIFY_NULLABLE_ANNOTATION (\w+)"""), "$1?")
            .replace(Regex("""$JSPECIFY_NULLNESS_UNSPECIFIED_ANNOTATION (\w+)"""), "$1")
            .replace("Object", "Any")
            .replace("void", "Unit")
            .replace("<? extends", "<out")
            .replace("<?>", "<*>")

    private fun transformValueParameters(valueParameters: String) =
        transformTypesByAnnotationsIfNeeded(valueParameters.replace(valueParameterRegex, "$2: $1$3"))

    private fun generateKotlinCode(parsedMethod: ParsedMethod, parsedCalls: Sequence<ParsedCall>) = buildString {
        val methodValueParameters = transformValueParameters(parsedMethod.valueParameters).removeSuffixIfPresent(" ".repeat(4))
        val methodTypeParameters = parsedMethod.typeParameters.replace(" extends ", " : ")

        appendLine("fun $methodTypeParameters${parsedMethod.funName}($methodValueParameters): ${parsedMethod.returnType} {")

        for (call in parsedCalls) {
            if (call.jspecifyMark != null) {
                appendLine(" ".repeat(4) + call.jspecifyMark)
            }
            append(" ".repeat(4) + call.receiver)
            for (callComponent in call.callComponents) {
                val typeArguments = callComponent.typeArguments?.let { transformTypesByAnnotationsIfNeeded(it) } ?: ""
                val valueArguments = callComponent.valueArguments?.let { "($it)" } ?: ""
                append(".${callComponent.callee}${typeArguments}$valueArguments")
            }
            append(call.indent)
        }

        append("}")
    }

    private fun generateKotlinCode(parsedOverrides: Pair<List<ParsedInterface>, Pair<String, String>?>): String {
        val (parsedInterfaces, topLevelClassInfo) = parsedOverrides
        val topLevelClass = topLevelClassInfo?.first
        val topLevelClassNullability = topLevelClassInfo?.second
        val defaultNotNullFromTopLevelClass = topLevelClassNullability == JSPECIFY_DEFAULT_NOT_NULL_ANNOTATION

        return buildString {
            for (interfaceWithOverrides in parsedInterfaces) {
                val superType = transformTypesByAnnotationsIfNeeded(interfaceWithOverrides.superType)
                val superTypeName = superType.split("<").first()
                val topLevelClassPrefix = if (topLevelClass != null && superTypeName != topLevelClass) "$topLevelClass." else ""

                appendLine("interface ${interfaceWithOverrides.name}Kt : $topLevelClassPrefix$superType {")

                for (override in interfaceWithOverrides.methods) {
                    append(" ".repeat(4))
                    val annotations = override.jspecifyAnnotation
                    val wasNullableAnnotation = annotations != null && annotations.matches(Regex("""^$JSPECIFY_NULLABLE_ANNOTATION\s*$"""))
                    val returnType = transformTypesByAnnotationsIfNeeded(override.returnType)
                    val valueParameters = transformValueParameters(override.valueParameters)
                    val returnTypeName = returnType.split("<").first()
                    val isReturnTypeNotSpecial = returnTypeName != "Any" && returnTypeName != "Unit"
                    val topLevelClassPrefixForReturnType =
                        if (topLevelClass != null && returnTypeName != topLevelClass && isReturnTypeNotSpecial) "$topLevelClass." else ""

                    appendLine("${override.jspecifyMark}override fun ${override.typeParameters}${override.funName}($valueParameters): $topLevelClassPrefixForReturnType${if (wasNullableAnnotation) "$returnType?" else returnType}")
                }

                appendLine("}")
            }
        }
    }

    private fun writeKotlinCode(javaFile: File, mode: String, code: String, modeDirective: String = "") {
        File("${javaFile.parentFile.parent}/kotlin/$mode/${javaFile.nameWithoutExtension}.kt")
            .writeText("// JAVA_SOURCES: ${javaFile.name}\n$modeDirective\n$code")
    }

    fun main() {
        val javaFiles = File(TESTS_DIRECTORY).walkTopDown().filter { it.name !in generationNotNeededFiles }

        for (file in javaFiles) {
            if (!file.isFile || file.extension != "java") continue

            val parsedMethod = parseMethod(file.readText())
            val parsedOverrides = parseOverrides(file.readText())
            val generatedKotlinCode = buildString {
                if (parsedMethod != null) {
                    val parsedCalls = parseCalls(parsedMethod.body)
                    append(generateKotlinCode(parsedMethod, parsedCalls))
                }
                if (parsedOverrides != null) {
                    append(generateKotlinCode(parsedOverrides))
                }
            }

            if (generatedKotlinCode.isNotEmpty()) {
                writeKotlinCode(file, "strictMode", generatedKotlinCode, "// JSPECIFY_STATE strict\n")
                writeKotlinCode(file, "warnMode", generatedKotlinCode)
            }
        }
    }
}

fun main() = GenerateKotlinUseSitesFromJavaOnesForJspecifyTests().main()
