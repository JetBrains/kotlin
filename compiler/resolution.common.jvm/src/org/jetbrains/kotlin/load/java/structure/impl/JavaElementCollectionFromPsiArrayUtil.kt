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
import org.jetbrains.kotlin.load.java.NULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.name.Name

inline fun <Psi, Java> Array<Psi>.convert(factory: (Psi) -> Java): List<Java> =
        when (size) {
            0 -> emptyList()
            1 -> listOf(factory(first()))
            else -> map(factory)
        }

fun <Psi, Java> Collection<Psi>.convert(factory: (Psi) -> Java): List<Java> =
        when (size) {
            0 -> emptyList()
            1 -> listOf(factory(first()))
            else -> map(factory)
        }

internal fun classes(classes: Array<PsiClass>, sourceFactory: JavaElementSourceFactory): Collection<JavaClass> =
        classes.convert { JavaClassImpl(sourceFactory.createPsiSource(it)) }

internal fun classes(classes: Collection<PsiClass>, sourceFactory: JavaElementSourceFactory): Collection<JavaClass> =
        classes.convert { JavaClassImpl(sourceFactory.createPsiSource(it)) }

internal fun packages(packages: Array<PsiPackage>, scope: GlobalSearchScope, sourceFactory: JavaElementSourceFactory): Collection<JavaPackage> =
        packages.convert { psi -> JavaPackageImpl(sourceFactory.createPsiSource(psi), scope) }

internal fun methods(methods: Collection<PsiMethod>, sourceFactory: JavaElementSourceFactory): Collection<JavaMethod> =
        methods.convert { JavaMethodImpl(sourceFactory.createPsiSource(it)) }

internal fun constructors(methods: Collection<PsiMethod>, sourceFactory: JavaElementSourceFactory): Collection<JavaConstructor> =
        methods.convert { JavaConstructorImpl(sourceFactory.createPsiSource(it)) }

internal fun fields(fields: Collection<PsiField>, sourceFactory: JavaElementSourceFactory): Collection<JavaField> =
        fields.convert { JavaFieldImpl(sourceFactory.createPsiSource(it)) }

internal fun valueParameters(parameters: Array<PsiParameter>, sourceFactory: JavaElementSourceFactory): List<JavaValueParameter> =
        parameters.convert { JavaValueParameterImpl(sourceFactory.createPsiSource(it)) }

internal fun typeParameters(typeParameters: Array<PsiTypeParameter>, sourceFactory: JavaElementSourceFactory): List<JavaTypeParameter> =
        typeParameters.convert { JavaTypeParameterImpl(sourceFactory.createPsiSource(it)) }

internal fun classifierTypes(classTypes: Array<PsiClassType>, sourceFactory: JavaElementSourceFactory): Collection<JavaClassifierType> =
        classTypes.convert { JavaClassifierTypeImpl(sourceFactory.createTypeSource(it)) }

internal fun annotations(annotations: Array<out PsiAnnotation>, sourceFactory: JavaElementSourceFactory): Collection<JavaAnnotation> =
        annotations.convert { JavaAnnotationImpl(sourceFactory.createPsiSource(it)) }

internal fun nullabilityAnnotations(annotations: Array<out PsiAnnotation>, sourceFactory: JavaElementSourceFactory): Collection<JavaAnnotation> =
        annotations.convert { JavaAnnotationImpl(sourceFactory.createPsiSource(it)) }
                .filter { annotation ->
                    val fqName = annotation.classId?.asSingleFqName() ?: return@filter false
                    fqName in NULLABILITY_ANNOTATIONS
                }


internal fun namedAnnotationArguments(nameValuePairs: Array<PsiNameValuePair>, sourceFactory: JavaElementSourceFactory): Collection<JavaAnnotationArgument> =
        nameValuePairs.convert { psi ->
            val name = psi.name?.let(Name::identifier)
            val value = psi.value ?: error("Annotation argument value cannot be null: $name")
            JavaAnnotationArgumentImpl.create(value, name, sourceFactory)
        }
