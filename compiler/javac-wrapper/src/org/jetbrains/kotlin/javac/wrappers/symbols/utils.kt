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

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import javax.lang.model.AnnotatedConstruct
import javax.lang.model.element.*

internal val Element.isAbstract: Boolean
    get() = modifiers.contains(Modifier.ABSTRACT)

internal val Element.isStatic: Boolean
    get() = modifiers.contains(Modifier.STATIC)

internal val Element.isFinal: Boolean
    get() = modifiers.contains(Modifier.FINAL)

internal fun Element.getVisibility(): Visibility = modifiers.getVisibility()

internal fun Set<Modifier>.getVisibility(): Visibility =
    when {
        Modifier.PUBLIC in this -> Visibilities.PUBLIC
        Modifier.PRIVATE in this -> Visibilities.PRIVATE
        Modifier.PROTECTED in this -> {
            if (Modifier.STATIC in this) {
                JavaVisibilities.PROTECTED_STATIC_VISIBILITY
            } else {
                JavaVisibilities.PROTECTED_AND_PACKAGE
            }
        }
        else -> JavaVisibilities.PACKAGE_VISIBILITY
    }


internal fun TypeElement.computeClassId(): ClassId? {
    val enclosingElement = enclosingElement
    if (enclosingElement.kind != ElementKind.PACKAGE) {
        val parentClassId = (enclosingElement as TypeElement).computeClassId() ?: return null
        return parentClassId.createNestedClassId(Name.identifier(simpleName.toString()))
    }

    return ClassId.topLevel(FqName(qualifiedName.toString()))
}

internal fun ExecutableElement.valueParameters(javac: JavacWrapper): List<JavaValueParameter> =
    parameters.let { parameters ->
        val isVarArgs = isVarArgs
        val lastIndex = parameters.lastIndex
        parameters.mapIndexed { index, parameter ->
            val simpleName = parameter.simpleName.toString()
            SymbolBasedValueParameter(
                parameter,
                if (!simpleName.contentEquals("arg$index")) simpleName else "p$index",
                index == lastIndex && isVarArgs,
                javac
            )
        }
    }

internal fun AnnotatedConstruct.findAnnotation(
    fqName: FqName,
    javac: JavacWrapper
) = annotationMirrors.find {
    (it.annotationType.asElement() as TypeElement).qualifiedName.toString() == fqName.asString()
}?.let { SymbolBasedAnnotation(it, javac) }