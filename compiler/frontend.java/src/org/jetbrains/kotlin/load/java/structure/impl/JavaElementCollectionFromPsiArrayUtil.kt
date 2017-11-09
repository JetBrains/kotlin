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

@file:JvmName("JavaElementCollectionFromPsiArrayUtil")

package org.jetbrains.kotlin.load.java.structure.impl

import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.Name

private inline fun <Psi, Java> Array<Psi>.convert(factory: (Psi) -> Java): List<Java> =
        when (size) {
            0 -> emptyList()
            1 -> listOf(factory(first()))
            else -> map(factory)
        }

private fun <Psi, Java> Collection<Psi>.convert(factory: (Psi) -> Java): List<Java> =
        when (size) {
            0 -> emptyList()
            1 -> listOf(factory(first()))
            else -> map(factory)
        }

internal fun classes(classes: Array<PsiClass>): Collection<JavaClass> =
        classes.convert(::JavaClassImpl)

internal fun classes(classes: Collection<PsiClass>): Collection<JavaClass> =
        classes.convert(::JavaClassImpl)

internal fun packages(packages: Array<PsiPackage>, scope: GlobalSearchScope): Collection<JavaPackage> =
        packages.convert { psi -> JavaPackageImpl(psi, scope) }

internal fun methods(methods: Collection<PsiMethod>): Collection<JavaMethod> =
        methods.convert(::JavaMethodImpl)

internal fun constructors(methods: Collection<PsiMethod>): Collection<JavaConstructor> =
        methods.convert(::JavaConstructorImpl)

internal fun fields(fields: Collection<PsiField>): Collection<JavaField> =
        fields.convert(::JavaFieldImpl)

internal fun valueParameters(parameters: Array<PsiParameter>): List<JavaValueParameter> =
        parameters.convert(::JavaValueParameterImpl)

internal fun typeParameters(typeParameters: Array<PsiTypeParameter>): List<JavaTypeParameter> =
        typeParameters.convert(::JavaTypeParameterImpl)

internal fun classifierTypes(classTypes: Array<PsiClassType>): Collection<JavaClassifierType> =
        classTypes.convert(::JavaClassifierTypeImpl)

internal fun annotations(annotations: Array<out PsiAnnotation>): Collection<JavaAnnotation> =
        annotations.convert(::JavaAnnotationImpl)

internal fun namedAnnotationArguments(nameValuePairs: Array<PsiNameValuePair>): Collection<JavaAnnotationArgument> =
        nameValuePairs.convert { psi ->
            val name = psi.name?.let(Name::identifier)
            val value = psi.value ?: error("Annotation argument value cannot be null: $name")
            JavaAnnotationArgumentImpl.create(value, name)
        }
