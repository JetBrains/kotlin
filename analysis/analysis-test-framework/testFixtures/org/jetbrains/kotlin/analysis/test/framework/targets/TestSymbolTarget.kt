/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.targets

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.analysis.test.framework.targets.TestSymbolTarget.ContextParameterTarget
import org.jetbrains.kotlin.analysis.test.framework.targets.TestSymbolTarget.TypeParameterTarget
import org.jetbrains.kotlin.analysis.test.framework.targets.TestSymbolTarget.ValueParameterTarget
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Path

/**
 * An identifier specified in test data that points to a target symbol. The target support makes it easy to refer to specific symbols in the
 * test data.
 *
 * #### Example
 *
 * ```kotlin
 * package test
 *
 * enum class E {
 *     A {
 *         val x: String = ""
 *     },
 *     B
 * }
 *
 * // class: test/E
 * ```
 *
 * The pseudo directive `class: test/E` specifies an enum [ClassTarget] with a [ClassId] of `test/E`.
 */
sealed interface TestSymbolTarget {
    data class PackageTarget(val packageFqName: FqName) : TestSymbolTarget

    data class ClassTarget(val classId: ClassId) : TestSymbolTarget

    data class ScriptTarget(val file: KtFile) : TestSymbolTarget

    data class TypeAliasTarget(val classId: ClassId) : TestSymbolTarget

    /**
     * Describes a class-like declaration with the specified [classId]. This target is a composition of [ClassTarget] and [TypeAliasTarget].
     */
    data class ClassLikeTarget(val classId: ClassId) : TestSymbolTarget

    /**
     * Describes a property or a function with the specified [callableId].
     */
    data class CallableTarget(val callableId: CallableId) : TestSymbolTarget

    /**
     * Describes a property with the specified [callableId].
     */
    data class PropertyTarget(val callableId: CallableId) : TestSymbolTarget

    /**
     * Describes a function with the specified [callableId] and an optional list of [parameterNames]:
     *
     * - `parameterNames == null` matches any function with [callableId];
     * - `parameterNames == emptyList()` matches only the no-arg overload;
     * - a non-empty list matches a function whose value parameter names equal to the given list.
     */
    data class FunctionTarget(val callableId: CallableId, val parameterNames: List<Name>?) : TestSymbolTarget

    /**
     * Describes a constructor of the class with the specified [classId] and an optional list of [parameterNames].
     * See [FunctionTarget] for the [parameterNames] semantics.
     */
    data class ConstructorTarget(val classId: ClassId, val parameterNames: List<Name>?) : TestSymbolTarget

    data class EnumEntryInitializerTarget(val enumEntryId: CallableId) : TestSymbolTarget

    data class SamConstructorTarget(val classId: ClassId) : TestSymbolTarget

    sealed interface TargetWithOwner : TestSymbolTarget {
        val ownerTarget: TestSymbolTarget
    }

    data class TypeParameterTarget(val name: Name, override val ownerTarget: TestSymbolTarget) : TargetWithOwner

    data class ValueParameterTarget(val name: Name, override val ownerTarget: TestSymbolTarget) : TargetWithOwner

    data class ContextParameterTarget(val name: Name, override val ownerTarget: TestSymbolTarget) : TargetWithOwner

    data class GetterTarget(override val ownerTarget: TestSymbolTarget) : TargetWithOwner

    data class SetterTarget(override val ownerTarget: TestSymbolTarget) : TargetWithOwner

    data class FieldTarget(val callableId: CallableId) : TestSymbolTarget

    companion object {
        private val identifiers = arrayOf(
            "package:",
            "callable:",
            "property:",
            "function:",
            "constructor:",
            "class:",
            "typealias:",
            "class_like:",
            "enum_entry_initializer:",
            "script",
            "sam_constructor:",
            "type_parameter:",
            "value_parameter:",
            "context_parameter:",
            "getter:",
            "setter:",
            "field:"
        )

        /**
         * Parses a [TestSymbolTarget] from the given test data file's content.
         *
         * @see create
         */
        fun parse(testDataPath: Path, contextFile: KtFile?): TestSymbolTarget {
            val testFileText = FileUtil.loadFile(testDataPath.toFile())
            val identifier = testFileText.lineSequence().first(::isIdentifier).removePrefix("// ")
            return create(identifier, contextFile)
        }

        private fun isIdentifier(line: String): Boolean =
            identifiers.any { identifier ->
                line.startsWith(identifier) || line.startsWith("// $identifier")
            }

        /**
         * Creates a [TestSymbolTarget] from the [content] of a test data comment, such as `"class: Declaration"`.
         *
         * @param contextFile The [TestSymbolTarget] must be created in the context of a [KtFile]. For example, the [ScriptTarget] does not
         *  have any kind of identifier, but rather points to the [KtScript][org.jetbrains.kotlin.psi.KtScript] declaration of its context
         *  [KtFile].
         */
        fun create(content: String, contextFile: KtFile?): TestSymbolTarget {
            val key = content.substringBefore(":")
            val value = content.substringAfter(":").trim()
            return when (key) {
                "script" -> ScriptTarget(contextFile ?: error("Context file must be provided for a script target"))
                "package" -> PackageTarget(extractPackageFqName(value))
                "class" -> ClassTarget(ClassId.fromString(value))
                "typealias" -> TypeAliasTarget(ClassId.fromString(value))
                "class_like" -> ClassLikeTarget(ClassId.fromString(value))
                "callable" -> CallableTarget(extractCallableId(value))
                "property" -> PropertyTarget(extractCallableId(value))
                "function" -> createFunctionTarget(value)
                "constructor" -> createConstructorTarget(value)
                "enum_entry_initializer" -> EnumEntryInitializerTarget(extractCallableId(value))
                "sam_constructor" -> SamConstructorTarget(ClassId.fromString(value))
                "type_parameter" -> createTypeParameterTarget(value, contextFile)
                "value_parameter" -> createValueParameterTarget(value, contextFile)
                "context_parameter" -> createContextParameterTarget(value, contextFile)
                "getter" -> GetterTarget(create(value, contextFile))
                "setter" -> SetterTarget(create(value, contextFile))
                "field" -> FieldTarget(extractCallableId(value))
                else -> error("Invalid target symbol kind `$key`. Expected one of: ${identifiers.joinToString(", ")}")
            }
        }
    }
}

private fun extractPackageFqName(content: String): FqName = FqName.fromSegments(content.split('.'))

private fun extractCallableId(fullName: String): CallableId {
    val name = if ('.' in fullName) fullName.substringAfterLast(".") else fullName.substringAfterLast('/')
    val [packageName, className] = run {
        val packageNameWithClassName = fullName.dropLast(name.length + 1)
        when {
            '.' in fullName ->
                packageNameWithClassName.substringBeforeLast('/') to packageNameWithClassName.substringAfterLast('/')
            else -> packageNameWithClassName to null
        }
    }
    return CallableId(FqName(packageName.replace('/', '.')), className?.let { FqName(it) }, Name.identifier(name))
}

private fun createFunctionTarget(content: String): TestSymbolTarget.FunctionTarget {
    val [callableIdText, parameterNames] = extractCallableHeadAndParameters(content)
    return TestSymbolTarget.FunctionTarget(extractCallableId(callableIdText), parameterNames)
}

private fun createConstructorTarget(content: String): TestSymbolTarget.ConstructorTarget {
    val [callableIdText, parameterNames] = extractCallableHeadAndParameters(content)
    require(callableIdText.endsWith(".init")) {
        "Constructor target must end with `.init`, got `$callableIdText`."
    }
    val classIdPart = callableIdText.removeSuffix(".init")
    return TestSymbolTarget.ConstructorTarget(ClassId.fromString(classIdPart), parameterNames)
}

private fun extractCallableHeadAndParameters(content: String): Pair<String, List<Name>?> {
    val leftParenIndex = content.indexOf('(')
    if (leftParenIndex < 0) {
        return content to null
    }

    require(content.endsWith(')')) { "Expected `)` at the end of `$content`." }
    val callableIdText = content.substring(0, leftParenIndex)
    val parameterText = content.substring(leftParenIndex + 1, content.length - 1).trim()
    val parameterNames = if (parameterText.isEmpty()) {
        emptyList()
    } else {
        parameterText.split(',').map { Name.identifier(it.trim()) }
    }
    return callableIdText to parameterNames
}

private fun createTypeParameterTarget(content: String, contextFile: KtFile?): TypeParameterTarget {
    val [typeParameterName, ownerTarget] = extractOwnerTarget(content, contextFile)
    return TypeParameterTarget(Name.identifier(typeParameterName), ownerTarget)
}

private fun createValueParameterTarget(content: String, contextFile: KtFile?): ValueParameterTarget {
    val [valueParameterName, ownerTarget] = extractOwnerTarget(content, contextFile)
    return ValueParameterTarget(Name.identifier(valueParameterName), ownerTarget)
}

private fun createContextParameterTarget(content: String, contextFile: KtFile?): ContextParameterTarget {
    val [contextParameterName, ownerTarget] = extractOwnerTarget(content, contextFile)
    return ContextParameterTarget(Name.identifier(contextParameterName), ownerTarget)
}

private fun extractOwnerTarget(content: String, contextFile: KtFile?): Pair<String, TestSymbolTarget> {
    val customData = content.substringBefore(":")
    val owner = content.substringAfter(":").trim()
    val ownerData = TestSymbolTarget.create(owner, contextFile)
    return customData to ownerData
}
