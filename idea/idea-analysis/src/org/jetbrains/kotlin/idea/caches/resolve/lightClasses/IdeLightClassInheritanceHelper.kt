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

package org.jetbrains.kotlin.idea.caches.resolve.lightClasses

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.ImpreciseResolveResult
import org.jetbrains.kotlin.asJava.ImpreciseResolveResult.*
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.LightClassInheritanceHelper
import org.jetbrains.kotlin.asJava.classes.defaultJavaAncestorQualifiedName
import org.jetbrains.kotlin.idea.search.PsiBasedClassResolver
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry

class IdeLightClassInheritanceHelper : LightClassInheritanceHelper {
    override fun isInheritor(
            lightClass: KtLightClass,
            baseClass: PsiClass,
            checkDeep: Boolean
    ): ImpreciseResolveResult {
        val classOrObject = lightClass.kotlinOrigin ?: return UNSURE
        val entries = classOrObject.superTypeListEntries
        val hasSuperClass = entries.any { it is KtSuperTypeCallEntry }
        if (baseClass.qualifiedName == classOrObject.defaultJavaAncestorQualifiedName() && (!hasSuperClass || checkDeep)) {
            return MATCH
        }
        if (checkDeep && baseClass.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT) {
            return MATCH
        }
        val amongEntries = isAmongEntries(baseClass, entries)
        return when {
            !checkDeep -> amongEntries
            amongEntries == MATCH -> MATCH
            else -> UNSURE
        }
    }

    private fun isAmongEntries(baseClass: PsiClass, entries: List<KtSuperTypeListEntry>): ImpreciseResolveResult {
        val psiBasedResolver = PsiBasedClassResolver(baseClass)
        entries@ for (entry in entries) {
            val reference: KtSimpleNameExpression = entry.typeAsUserType?.referenceExpression ?: continue@entries
            when (psiBasedResolver.canBeTargetReference(reference)) {
                MATCH -> return MATCH
                NO_MATCH -> continue@entries
                UNSURE -> return UNSURE
            }
        }
        return NO_MATCH
    }
}
