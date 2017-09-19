/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.native.interop.gen

import kotlin.reflect.KProperty

interface KotlinScope {
    /**
     * @return the string to be used to reference the classifier in current scope.
     */
    fun reference(classifier: Classifier): String

    /**
     * @return the string to be used as a name in the declaration of the classifier in current scope.
     */
    fun declare(classifier: Classifier): String
}

data class Classifier(
        val pkg: String,
        val topLevelName: String,
        private val nestedNames: List<String> = emptyList()
) {

    companion object {
        fun topLevel(pkg: String, name: String): Classifier {
            assert(!name.contains('.'))
            assert(!name.contains('`'))
            return Classifier(pkg, name)
        }
    }

    val isTopLevel: Boolean get() = this.nestedNames.isEmpty()

    fun nested(name: String): Classifier {
        assert(!name.contains('.'))
        assert(!name.contains('`'))
        return this.copy(nestedNames = nestedNames + name)
    }

    val relativeFqName: String get() = buildString {
        append(topLevelName.asSimpleName())
        nestedNames.forEach {
            append('.')
            append(it.asSimpleName())
        }
    }

    val fqName: String get() = buildString {
        if (pkg.isNotEmpty()) {
            append(pkg)
            append('.')
        }
        append(relativeFqName)
    }
}

val Classifier.type
    get() = KotlinClassifierType(this, arguments = emptyList(), nullable = false)

fun Classifier.typeWith(vararg arguments: KotlinTypeArgument) =
        KotlinClassifierType(this, arguments.toList(), nullable = false)

interface KotlinTypeArgument {
    /**
     * @return the string to be used in the given scope to denote this.
     */
    fun render(scope: KotlinScope): String
}

object StarProjection : KotlinTypeArgument {
    override fun render(scope: KotlinScope) = "*"
}

interface KotlinType : KotlinTypeArgument

data class KotlinClassifierType(
        val classifier: Classifier,
        val arguments: List<KotlinTypeArgument>,
        val nullable: Boolean
) : KotlinType {

    override fun render(scope: KotlinScope): String = buildString {
        append(scope.reference(classifier))
        if (arguments.isNotEmpty()) {
            append('<')
            arguments.joinTo(this) { it.render(scope) }
            append('>')
        }
        if (nullable) {
            append('?')
        }
    }
}

fun KotlinClassifierType.makeNullableAsSpecified(nullable: Boolean) = if (this.nullable == nullable) {
    this
} else {
    this.copy(nullable = nullable)
}

fun KotlinClassifierType.makeNullable() = this.makeNullableAsSpecified(true)

data class KotlinFunctionType(
        val parameterTypes: List<KotlinType>,
        val returnType: KotlinType
) : KotlinType {

    override fun render(scope: KotlinScope) = buildString {
        append('(')
        parameterTypes.joinTo(this) { it.render(scope) }
        append(") -> ")
        append(returnType.render(scope))
    }
}

internal val cnamesStructsPackageName = "cnames.structs"

object KotlinTypes {
    val boolean by BuiltInType
    val byte by BuiltInType
    val short by BuiltInType
    val int by BuiltInType
    val long by BuiltInType
    val float by BuiltInType
    val double by BuiltInType
    val unit by BuiltInType
    val string by BuiltInType
    val any by BuiltInType

    val nativePtr by InteropType

    val cOpaque by InteropType
    val cOpaquePointer by InteropType
    val cOpaquePointerVar by InteropType

    val booleanVarOf by InteropClassifier

    val objCObject by InteropClassifier
    val objCObjectMeta by InteropClassifier
    val objCClass by InteropClassifier

    val cValuesRef by InteropClassifier

    val cPointer by InteropClassifier
    val cPointerVar by InteropClassifier
    val cArrayPointer by InteropClassifier
    val cArrayPointerVar by InteropClassifier
    val cPointerVarOf by InteropClassifier

    val cFunction by InteropClassifier

    val objCObjectVar by InteropClassifier
    val objCStringVarOf by InteropClassifier

    val objCObjectBase by InteropClassifier
    val objCObjectBaseMeta by InteropClassifier

    val cValue by InteropClassifier

    private object BuiltInType {
        operator fun getValue(thisRef: KotlinTypes, property: KProperty<*>): KotlinClassifierType =
                Classifier.topLevel("kotlin", property.name.capitalize()).type
    }

    private object InteropClassifier {
        operator fun getValue(thisRef: KotlinTypes, property: KProperty<*>): Classifier =
                Classifier.topLevel("kotlinx.cinterop", property.name.capitalize())
    }

    private object InteropType {
        operator fun getValue(thisRef: KotlinTypes, property: KProperty<*>): KotlinClassifierType =
                InteropClassifier.getValue(thisRef, property).type
    }
}

class KotlinFile(
        val pkg: String,
        namesToBeDeclared: List<String>
) : KotlinScope {

    // Note: all names are related to classifiers currently.

    private val namesToBeDeclared: Set<String>

    init {
        this.namesToBeDeclared = mutableSetOf()

        namesToBeDeclared.forEach {
            if (it in this.namesToBeDeclared) {
                throw IllegalArgumentException("'$it' is going to be declared twice")
            } else {
                this.namesToBeDeclared.add(it)
            }
        }
    }

    private val importedNameToPkg = mutableMapOf<String, String>()

    override fun reference(classifier: Classifier): String = if (classifier.topLevelName in namesToBeDeclared) {
        if (classifier.pkg == this.pkg) {
            classifier.relativeFqName
        } else {
            // Don't import if would clash with own declaration:
            classifier.fqName
        }
    } else if (classifier.pkg == this.pkg) {
        throw IllegalArgumentException(
                "'${classifier.topLevelName}' from the file package was not reserved for declaration"
        )
    } else {
        val pkg = importedNameToPkg.getOrPut(classifier.topLevelName) { classifier.pkg }
        if (pkg == classifier.pkg) {
            // Is successfully imported:
            classifier.relativeFqName
        } else {
            classifier.fqName
        }
    }

    private val alreadyDeclared = mutableSetOf<String>()

    override fun declare(classifier: Classifier): String {
        if (classifier.pkg != this.pkg) {
            throw IllegalArgumentException("wrong package; expected '$pkg', got '${classifier.pkg}'")
        }

        if (!classifier.isTopLevel) {
            throw IllegalArgumentException(
                    "'${classifier.relativeFqName}' is not top-level thus can't be declared at file scope"
            )
        }

        val topLevelName = classifier.topLevelName
        if (topLevelName in alreadyDeclared) {
            throw IllegalStateException("'$topLevelName' is already declared")
        }
        alreadyDeclared.add(topLevelName)

        return topLevelName.asSimpleName()
    }

    fun buildImports(): List<String> = importedNameToPkg.mapNotNull { (name, pkg) ->
        if (pkg == "kotlin" || pkg == "kotlinx.cinterop") {
            // Is already imported either by default or with '*':
            null
        } else {
            "import $pkg.${name.asSimpleName()}"
        }
    }.sorted()

}
