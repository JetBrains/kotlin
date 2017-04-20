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

import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import javax.lang.model.element.TypeElement
import javax.lang.model.element.TypeParameterElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable

class SymbolBasedClassifierType<out T : TypeMirror>(typeMirror: T,
                                                    javac: JavacWrapper) : SymbolBasedType<T>(typeMirror, javac), JavaClassifierType {

    override val classifier
        get() = when (typeMirror.kind) {
            TypeKind.DECLARED -> SymbolBasedClass((typeMirror as DeclaredType).asElement() as TypeElement, javac)
            TypeKind.TYPEVAR -> SymbolBasedTypeParameter((typeMirror as TypeVariable).asElement() as TypeParameterElement, javac)
            else -> null
        }

    override val typeArguments
        get() = if (typeMirror.kind == TypeKind.DECLARED) {
            (typeMirror as DeclaredType).typeArguments.map { create(it, javac) }
        } else {
            emptyList()
        }

    override val isRaw
        get() = when {
            typeMirror !is DeclaredType -> false
            (typeMirror.asElement() as TypeElement).typeParameters.isEmpty() -> false
            else -> typeMirror.typeArguments.isEmpty()
        }

    override val canonicalText
        get() = typeMirror.toString()

    override val presentableText
        get() = typeMirror.toString()

}
