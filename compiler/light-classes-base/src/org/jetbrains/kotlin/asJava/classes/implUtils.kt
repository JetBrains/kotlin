/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiReferenceList
import com.intellij.psi.impl.light.LightElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.psi.*

fun KtSuperTypeList.findEntry(fqNameToFind: String): KtSuperTypeListEntry? {
    val name = fqNameToFind.substringAfterLast(delimiter = '.', missingDelimiterValue = "")
    if (name.isEmpty()) {
        return entries.find { it.typeAsUserType?.textMatches(fqNameToFind) == true }
    }

    val qualifier = fqNameToFind.substringBeforeLast('.')
    val entries = entries.mapNotNull { entry -> entry.typeAsUserType?.let { entry to it } }
        .filter { (_, type) -> type.referencedName == name && (type.qualifier?.textMatches(qualifier) != false) }
        .ifEmpty { return null }

    return if (entries.size == 1) {
        entries.first().first
    } else {
        val entry = entries.firstOrNull { it.second.qualifier != null } ?: entries.firstOrNull()
        entry?.first
    }
}

// NOTE: avoid using blocking lazy in light classes, it leads to deadlocks
fun <T> lazyPub(initializer: () -> T) = lazy(LazyThreadSafetyMode.PUBLICATION, initializer)

@Suppress("UnusedReceiverParameter")
fun LightElement.cannotModify(): Nothing {
    throw IncorrectOperationException("Modification not implemented.")
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

fun KtClassOrObject.getExternalDependencies(): List<ModificationTracker> {
    return with(KotlinModificationTrackerService.getInstance(project)) {
        if (!this@getExternalDependencies.isLocal) return listOf(outOfBlockModificationTracker)
        else when (val file = containingFile) {
            is KtFile -> listOf(outOfBlockModificationTracker, fileModificationTracker(file))
            else -> listOf(outOfBlockModificationTracker)
        }
    }
}
