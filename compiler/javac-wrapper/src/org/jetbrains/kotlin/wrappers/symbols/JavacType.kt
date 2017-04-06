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

package org.jetbrains.kotlin.wrappers.symbols

import org.jetbrains.kotlin.javac.Javac
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationOwner
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.name.FqName
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

open class JavacType<out T : TypeMirror>(val typeMirror: T,
                                         val javac: Javac) : JavaType, JavaAnnotationOwner {

    companion object {
        fun <TM : TypeMirror> create(t: TM, javac: Javac) = when {
            t.kind.isPrimitive || t.toString() == "void" -> JavacPrimitiveType(t, javac)
            t.kind == TypeKind.DECLARED || t.kind == TypeKind.TYPEVAR -> JavacClassifierType(t, javac)
            t.kind == TypeKind.WILDCARD -> JavacWildcardType(t, javac)
            t.kind == TypeKind.ARRAY -> JavacArrayType(t, javac)
            else -> throw UnsupportedOperationException("Unsupported type: $t")
        }
    }

    override val annotations: Collection<JavaAnnotation> = emptyList()

    override val isDeprecatedInJavaDoc
        get() = javac.isDeprecated(typeMirror)

    override fun findAnnotation(fqName: FqName) = null

    override fun equals(other: Any?) = (other as? JavacType<*>)?.typeMirror == typeMirror

    override fun hashCode() = typeMirror.hashCode()

    override fun toString() = typeMirror.toString()

}