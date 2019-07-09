/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.psi.KtEnumEntry

internal class KtUltraLightClassForEnumEntry(
    enumEntry: KtEnumEntry, support: KtUltraLightSupport,
    private val enumConstant: PsiEnumConstant
) : KtUltraLightClass(enumEntry, support), PsiEnumConstantInitializer {

    private val baseClassReferenceAndType: Pair<PsiJavaCodeReferenceElement, PsiClassType> by lazyPub {
        // It should not be null for not-too-complex classes and that is not the case because
        // the containing class is not too complex (since we created KtUltraLightClassForEnumEntry instance)
        val extendsList =
            super.getExtendsList() ?: error("KtUltraLightClass::getExtendsList is null for ${enumEntry.fqName}")

        Pair(
            extendsList.referenceElements.getOrNull(0) ?: error("No referenceElements found for ${enumEntry.fqName}"),
            extendsList.referencedTypes.getOrNull(0) ?: error("No referencedTypes found for ${enumEntry.fqName}")
        )
    }

    override fun getBaseClassType() = baseClassReferenceAndType.second

    override fun getBaseClassReference() = baseClassReferenceAndType.first

    override fun getArgumentList(): PsiExpressionList? = null

    override fun getEnumConstant() = enumConstant

    override fun isInQualifiedNew() = false
}
