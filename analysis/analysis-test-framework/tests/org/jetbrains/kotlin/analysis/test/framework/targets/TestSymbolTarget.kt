/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.targets

import com.intellij.openapi.util.io.FileUtil
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

    data class CallableTarget(val callableId: CallableId) : TestSymbolTarget

    data class EnumEntryInitializerTarget(val enumEntryId: CallableId) : TestSymbolTarget

    data class SamConstructorTarget(val classId: ClassId) : TestSymbolTarget

    sealed interface TargetWithOwner : TestSymbolTarget {
        val ownerTarget: TestSymbolTarget
    }

    data class TypeParameterTarget(val name: Name, override val ownerTarget: TestSymbolTarget) : TargetWithOwner

    data class ValueParameterTarget(val name: Name, override val ownerTarget: TestSymbolTarget) : TargetWithOwner

    companion object {
        private val identifiers = arrayOf(
            "package:",
            "callable:",
            "class:",
            "typealias:",
            "enum_entry_initializer:",
            "script",
            "sam_constructor:",
            "type_parameter:",
            "value_parameter:",
        )

        /**
         * Parses a [TestSymbolTarget] from the given test data file's content.
         *
         * @see create
         */
        fun parse(testDataPath: Path, contextFile: KtFile): TestSymbolTarget {
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
        fun create(content: String, contextFile: KtFile): TestSymbolTarget {
            val key = content.substringBefore(":")
            val value = content.substringAfter(":").trim()
            return when (key) {
                "script" -> ScriptTarget(contextFile)
                "package" -> PackageTarget(extractPackageFqName(value))
                "class" -> ClassTarget(ClassId.fromString(value))
                "typealias" -> TypeAliasTarget(ClassId.fromString(value))
                "callable" -> CallableTarget(extractCallableId(value))
                "enum_entry_initializer" -> EnumEntryInitializerTarget(extractCallableId(value))
                "sam_constructor" -> SamConstructorTarget(ClassId.fromString(value))
                "type_parameter" -> createTypeParameterTarget(value, contextFile)
                "value_parameter" -> createValueParameterTarget(value, contextFile)
                else -> error("Invalid target symbol kind `$key`. Expected one of: ${identifiers.joinToString(", ")}")
            }
        }
    }
}

private fun extractPackageFqName(content: String): FqName = FqName.fromSegments(content.split('.'))

private fun extractCallableId(fullName: String): CallableId {
    val name = if ('.' in fullName) fullName.substringAfterLast(".") else fullName.substringAfterLast('/')
    val (packageName, className) = run {
        val packageNameWithClassName = fullName.dropLast(name.length + 1)
        when {
            '.' in fullName ->
                packageNameWithClassName.substringBeforeLast('/') to packageNameWithClassName.substringAfterLast('/')
            else -> packageNameWithClassName to null
        }
    }
    return CallableId(FqName(packageName.replace('/', '.')), className?.let { FqName(it) }, Name.identifier(name))
}


private fun createTypeParameterTarget(content: String, contextFile: KtFile): TypeParameterTarget {
    val (typeParameterName, ownerTarget) = extractOwnerTarget(content, contextFile)
    return TypeParameterTarget(Name.identifier(typeParameterName), ownerTarget)
}

private fun createValueParameterTarget(content: String, contextFile: KtFile): ValueParameterTarget {
    val (valueParameterName, ownerTarget) = extractOwnerTarget(content, contextFile)
    return ValueParameterTarget(Name.identifier(valueParameterName), ownerTarget)
}

private fun extractOwnerTarget(content: String, contextFile: KtFile): Pair<String, TestSymbolTarget> {
    val customData = content.substringBefore(":")
    val owner = content.substringAfter(":").trim()
    val ownerData = TestSymbolTarget.create(owner, contextFile)
    return customData to ownerData
}
