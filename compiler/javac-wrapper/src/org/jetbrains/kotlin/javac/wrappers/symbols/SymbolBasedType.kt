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

package org.jetbrains.kotlin.javac.wrappers.symbols

import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationOwner
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.name.FqName
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

open class SymbolBasedType<out T : TypeMirror>(val typeMirror: T,
                                               val javac: JavacWrapper) : JavaType, JavaAnnotationOwner {

    companion object {
        fun <TM : TypeMirror> create(t: TM, javac: JavacWrapper) = when {
            t.kind.isPrimitive || t.toString() == "void" -> SymbolBasedPrimitiveType(t, javac)
            t.kind == TypeKind.DECLARED || t.kind == TypeKind.TYPEVAR -> SymbolBasedClassifierType(t, javac)
            t.kind == TypeKind.WILDCARD -> SymbolBasedWildcardType(t, javac)
            t.kind == TypeKind.ARRAY -> SymbolBasedArrayType(t, javac)
            else -> throw UnsupportedOperationException("Unsupported type: $t")
        }
    }

    override val annotations
        get() = typeMirror.annotationMirrors.map { SymbolBasedAnnotation(it, javac) }

    override val isDeprecatedInJavaDoc
        get() = javac.isDeprecated(typeMirror)

    override fun findAnnotation(fqName: FqName) = typeMirror.annotationMirrors
            .find { it.toString() == "@${fqName.asString()}" }
            ?.let { SymbolBasedAnnotation(it, javac) }

    override fun equals(other: Any?) = (other as? SymbolBasedType<*>)?.typeMirror == typeMirror

    override fun hashCode() = typeMirror.hashCode()

    override fun toString() = typeMirror.toString()

}