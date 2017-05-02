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

import com.sun.tools.javac.code.Symbol
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import javax.lang.model.element.TypeElement
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.type.*

sealed class SymbolBasedType<out T : TypeMirror>(val typeMirror: T,
                                                 val javac: JavacWrapper) : JavaType, JavaAnnotationOwner {

    companion object {
        fun <TM : TypeMirror> create(t: TM, javac: JavacWrapper) = when {
            t.kind.isPrimitive || t.kind == TypeKind.VOID -> SymbolBasedPrimitiveType(t, javac)
            t.kind == TypeKind.DECLARED || t.kind == TypeKind.TYPEVAR -> SymbolBasedClassifierType(t, javac)
            t.kind == TypeKind.WILDCARD -> SymbolBasedWildcardType(t as WildcardType, javac)
            t.kind == TypeKind.ARRAY -> SymbolBasedArrayType(t as ArrayType, javac)
            else -> throw UnsupportedOperationException("Unsupported type: $t")
        }
    }

    override val annotations: Collection<JavaAnnotation>
        get() = typeMirror.annotationMirrors.map { SymbolBasedAnnotation(it, javac) }

    override val isDeprecatedInJavaDoc: Boolean
        get() = javac.isDeprecated(typeMirror)

    override fun findAnnotation(fqName: FqName) = typeMirror.findAnnotation(fqName, javac)

    override fun equals(other: Any?) = (other as? SymbolBasedType<*>)?.typeMirror == typeMirror

    override fun hashCode() = typeMirror.hashCode()

    override fun toString() = typeMirror.toString()

}

class SymbolBasedPrimitiveType(typeMirror: TypeMirror,
                               javac: JavacWrapper) : SymbolBasedType<TypeMirror>(typeMirror, javac), JavaPrimitiveType {

    override val type: PrimitiveType?
        get() = if (typeMirror.kind == TypeKind.VOID) null else JvmPrimitiveType.get(typeMirror.toString()).primitiveType

}

class SymbolBasedClassifierType<out T : TypeMirror>(typeMirror: T,
                                                    javac: JavacWrapper) : SymbolBasedType<T>(typeMirror, javac), JavaClassifierType {

    override val classifier: JavaClassifier?
        get() = when (typeMirror.kind) {
            TypeKind.DECLARED -> ((typeMirror as DeclaredType).asElement() as Symbol.ClassSymbol).let {
                SymbolBasedClass(it, javac, it.classfile)
            }
            TypeKind.TYPEVAR -> SymbolBasedTypeParameter((typeMirror as TypeVariable).asElement() as TypeParameterElement, javac)
            else -> null
        }

    override val typeArguments: List<JavaType>
        get() = if (typeMirror.kind == TypeKind.DECLARED) {
            (typeMirror as DeclaredType).typeArguments.map { create(it, javac) }
        } else {
            emptyList()
        }

    override val isRaw: Boolean
        get() = when {
            typeMirror !is DeclaredType -> false
            (typeMirror.asElement() as TypeElement).typeParameters.isEmpty() -> false
            else -> typeMirror.typeArguments.isEmpty()
        }

    override val classifierQualifiedName: String
        get() = typeMirror.toString()

    override val presentableText: String
        get() = typeMirror.toString()

}

class SymbolBasedWildcardType(typeMirror: WildcardType,
                              javac: JavacWrapper) : SymbolBasedType<WildcardType>(typeMirror, javac), JavaWildcardType {

    override val bound: JavaType?
        get() = typeMirror.let {
            val boundMirror = it.extendsBound ?: it.superBound
            boundMirror?.let { create(it, javac) }
        }

    override val isExtends: Boolean
        get() = typeMirror.extendsBound != null

}

class SymbolBasedArrayType(typeMirror: ArrayType,
                           javac: JavacWrapper) : SymbolBasedType<ArrayType>(typeMirror, javac), JavaArrayType {

    override val componentType: JavaType
        get() = create(typeMirror.componentType, javac)

}