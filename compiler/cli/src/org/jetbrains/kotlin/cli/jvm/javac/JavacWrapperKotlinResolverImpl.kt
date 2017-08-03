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

package org.jetbrains.kotlin.cli.jvm.javac

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.javac.JavacWrapperKotlinResolver
import org.jetbrains.kotlin.javac.resolve.MockKotlinField
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject

class JavacWrapperKotlinResolverImpl(private val lightClassGenerationSupport: CliLightClassGenerationSupport) : JavacWrapperKotlinResolver {

    private val cache = hashMapOf<KtClassOrObject, KtLightClass>()

    override fun resolveSupertypes(classOrObject: KtClassOrObject): List<ClassId> {
        val lightClass = classOrObject.getLightClass() ?: return emptyList()

        return lightClass.superTypes
                .mapNotNull { it.resolve()?.computeClassId() }
    }

    override fun findField(classOrObject: KtClassOrObject, name: String): JavaField? {
        val lightClass = classOrObject.getLightClass() ?: return null

        return lightClass.allFields.find { it.name == name}?.let(::MockKotlinField)
    }

    private fun KtClassOrObject.getLightClass(): KtLightClass? =
        cache[this] ?: lightClassGenerationSupport.getLightClass(this)?.also { cache[this] = it }

    private fun PsiClass.computeClassId(): ClassId? =
            containingClass?.computeClassId()?.createNestedClassId(Name.identifier(name!!)) ?: qualifiedName?.let { ClassId.topLevel(FqName(it)) }

}