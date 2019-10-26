/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiReferenceList
import com.intellij.psi.impl.light.LightElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

// NOTE: avoid using blocking lazy in light classes, it leads to deadlocks
fun <T> lazyPub(initializer: () -> T) = lazy(LazyThreadSafetyMode.PUBLICATION, initializer)

fun LightElement.cannotModify(): Nothing {
    throw IncorrectOperationException("Modification not implemented.")
}

fun KtSuperTypeList.findEntry(fqNameToFind: String): KtSuperTypeListEntry? {
    val context = LightClassGenerationSupport.getInstance(project).analyzeWithContent(parent as KtClassOrObject)
    return entries.firstOrNull {
        val referencedType = context[BindingContext.TYPE, it.typeReference]
        referencedType?.constructor?.declarationDescriptor?.fqNameUnsafe?.asString() == fqNameToFind
    }
}

fun PsiReferenceList.addSuperTypeEntry(
    superTypeList: KtSuperTypeList,
    entry: KtSuperTypeListEntry,
    reference: PsiJavaCodeReferenceElement
) {
    // Only classes may be mentioned in 'extends' list, thus create super call instead simple type reference
    val entryToAdd =
        if ((reference.parent as? PsiReferenceList)?.role == PsiReferenceList.Role.IMPLEMENTS_LIST && role == PsiReferenceList.Role.EXTENDS_LIST) {
            KtPsiFactory(this).createSuperTypeCallEntry("${entry.text}()")
        } else entry
    // TODO: implement KtSuperListEntry qualification/shortening when inserting reference from another context
    if (entry.parent != superTypeList) {
        superTypeList.addEntry(entryToAdd)
    } else {
        // Preserve original entry order
        entry.replace(entryToAdd)
    }
}

internal fun KtClassOrObject.getExternalDependencies(): List<ModificationTracker> {
    return with(KotlinModificationTrackerService.getInstance(project)) {
        if (!safeIsLocal()) return listOf(outOfBlockModificationTracker)
        else when (val file = containingFile) {
            is KtFile -> listOf(outOfBlockModificationTracker, fileModificationTracker(file))
            else -> listOf(outOfBlockModificationTracker)
        }
    }
}