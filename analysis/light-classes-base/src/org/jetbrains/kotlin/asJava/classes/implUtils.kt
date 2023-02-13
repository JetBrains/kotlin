/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.*
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
            KtPsiFactory(project).createSuperTypeCallEntry("${entry.text}()")
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

// There is no other known way found to make PSI types annotated for now (without creating a new instance).
// It seems we need for platform changes to do it more convenient way (KTIJ-141).
private val setPsiTypeAnnotationProvider: (PsiType, TypeAnnotationProvider) -> Unit by lazyPub {
    val klass = PsiType::class.java
    val providerField = try {
        klass.getDeclaredField("myAnnotationProvider")
            .also { it.isAccessible = true }
    } catch (e: NoSuchFieldException) {
        if (ApplicationManager.getApplication().isInternal) throw e
        null
    } catch (e: SecurityException) {
        if (ApplicationManager.getApplication().isInternal) throw e
        null
    }

    { psiType, provider ->
        providerField?.set(psiType, provider)
    }
}

fun PsiType.annotateByTypeAnnotationProvider(
    annotations: Sequence<List<PsiAnnotation>>,
): PsiType {
    val annotationsIterator = annotations.iterator()
    if (!annotationsIterator.hasNext()) return this

    if (this is PsiPrimitiveType) {
        val typeAnnotation = annotationsIterator.next()
        return if (typeAnnotation.isEmpty()) {
            this
        } else {
            val provider = TypeAnnotationProvider.Static.create(typeAnnotation.toTypedArray())
            // NB: [PsiType#annotate] makes a clone of the given [PsiType] while manipulating the type annotation provider.
            // For simple primitive type, it will be okay and intuitive (than reflection hack).
            annotate(provider)
        }
    }

    fun recursiveAnnotator(psiType: PsiType) {
        if (!annotationsIterator.hasNext()) return
        val typeAnnotations = annotationsIterator.next()

        when (psiType) {
            is PsiPrimitiveType -> return // Primitive type cannot be type parameter so we skip it
            is PsiClassType -> {
                for (parameterType in psiType.parameters) {
                    recursiveAnnotator(parameterType)
                }
            }
            is PsiArrayType ->
                recursiveAnnotator(psiType.componentType)
            else -> {}
        }

        if (typeAnnotations.isEmpty()) return

        val provider = TypeAnnotationProvider.Static.create(typeAnnotations.toTypedArray())
        setPsiTypeAnnotationProvider(psiType, provider)
    }

    recursiveAnnotator(this)
    return this
}
