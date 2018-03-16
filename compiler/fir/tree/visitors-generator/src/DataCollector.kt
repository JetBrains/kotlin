/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

class DataCollector {

    private val references = mutableMapOf<String, List<String>>()
    private val packagePerClass = mutableMapOf<String, FqName>()
    private val baseTransformedTypes = mutableListOf<String>()

    fun readFile(file: KtFile) {
        file.acceptChildren(object : KtVisitorVoid() {

            override fun visitKtElement(element: KtElement) {
                super.visitKtElement(element)
                element.acceptChildren(this)
            }

            override fun visitClass(klass: KtClass) {
                val className = klass.name
                if (klass.isInterface() && className != null) {
                    packagePerClass[className] = file.packageFqName
                    val isBaseTT = klass.annotationEntries.any {
                        it.shortName?.asString() == BASE_TRANSFORMED_TYPE_ANNOTATION_NAME
                    }
                    if (isBaseTT) {
                        baseTransformedTypes += className
                    }

                    val manual = klass.superTypeListEntries.find {
                        it.typeReference?.findDescendantOfType<KtAnnotationEntry> { annotationEntry ->
                            annotationEntry.shortName?.asString() == VISITED_SUPERTYPE_ANNOTATION_NAME
                        } != null
                    }
                    if (manual != null) {
                        references[className] = listOfNotNull(manual.typeAsUserType?.referencedName)
                    } else {
                        references[className] =
                                klass.superTypeListEntries.mapNotNull {
                                    it.typeAsUserType?.referencedName
                                }
                    }
                }

                super.visitClass(klass)
            }
        })
    }


    private fun Map<String, List<String>>.computeBackReferences(): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()

        this.forEach { (k, v) ->
            v.forEach {
                result.merge(it, listOf(k)) { a, b -> a + b }
            }
        }
        return result
    }

    private fun Map<String, List<String>>.sorted(): Map<String, List<String>> {
        return this.toSortedMap().mapValues { (_, v) -> v.sorted() }
    }

    fun computeResult(): ReferencesData {
        val back = references.computeBackReferences()

        val keysToKeep =
            generateSequence(listOf(FIR_ELEMENT_CLASS_NAME)) { firName ->
                firName.flatMap { back[it].orEmpty() }.takeUnless { it.isEmpty() }
            }.flatten().toSet()

        val cleanBack = back.filterKeys { it in keysToKeep }
        return ReferencesData(
            cleanBack.computeBackReferences().sorted(),
            cleanBack.sorted(),
            packagePerClass.filterKeys { it in keysToKeep }.values.distinct().sortedBy { it.asString() },
            baseTransformedTypes.sorted()
        )
    }

    data class ReferencesData(
        val direct: Map<String, List<String>>,
        val back: Map<String, List<String>>,
        val usedPackages: List<FqName>,
        val baseTransformedTypes: List<String>
    )
}