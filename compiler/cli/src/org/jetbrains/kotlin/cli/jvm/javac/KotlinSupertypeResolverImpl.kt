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
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.javac.KotlinSupertypeResolver
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject

class KotlinSupertypeResolverImpl(private val lightClassGenerationSupport: CliLightClassGenerationSupport) : KotlinSupertypeResolver {

    override fun resolveSupertypes(classOrObject: KtClassOrObject): List<ClassId> {
        val lightClass = lightClassGenerationSupport.getLightClass(classOrObject) ?: return emptyList()

        return lightClass.superTypes
                .mapNotNull { it.resolve()?.computeClassId() }
    }

    private fun PsiClass.computeClassId(): ClassId? =
            containingClass?.computeClassId()?.createNestedClassId(Name.identifier(name!!)) ?: qualifiedName?.let { ClassId.topLevel(FqName(it)) }

}