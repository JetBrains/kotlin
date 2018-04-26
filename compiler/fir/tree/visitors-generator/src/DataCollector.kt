/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

class DataCollector {

    data class NameWithTypeParameters(val name: String, val typeParameters: List<String>) : Comparable<NameWithTypeParameters> {
        override fun compareTo(other: NameWithTypeParameters): Int = name.compareTo(other.name)

        constructor(name: String, typeParameterString: String) :
                this(name, typeParameterString.drop(1).dropLast(1).split(" ", "\t", ",").filter { it.isNotEmpty() })

        constructor(name: String) : this(name, emptyList())

        override fun toString(): String =
            name + if (typeParameters.isEmpty()) "" else typeParameters.joinToString(prefix = "<", postfix = ">", separator = ", ")
    }

    private val references = mutableMapOf<NameWithTypeParameters, List<NameWithTypeParameters>>()
    private val packagePerClass = mutableMapOf<NameWithTypeParameters, FqName>()
    private val baseTransformedTypes = mutableListOf<NameWithTypeParameters>()

    private fun KtSuperTypeListEntry.toNameWithTypeParameters(): NameWithTypeParameters? {
        val type = typeAsUserType ?: return null
        val name = type.referencedName ?: return null
        val typeArguments = type.typeArgumentList?.text ?: ""
        return NameWithTypeParameters(name, typeArguments)
    }

    fun readFile(file: KtFile) {
        file.acceptChildren(object : KtVisitorVoid() {

            override fun visitKtElement(element: KtElement) {
                super.visitKtElement(element)
                element.acceptChildren(this)
            }

            override fun visitClass(klass: KtClass) {
                val className = klass.name ?: run {
                    super.visitClass(klass)
                    return
                }
                val classNameWithParameters = NameWithTypeParameters(className, klass.typeParameterList?.text ?: "")
                if (klass.isInterface()) {
                    packagePerClass[classNameWithParameters] = file.packageFqName
                    val isBaseTT = klass.annotationEntries.any {
                        it.shortName?.asString() == BASE_TRANSFORMED_TYPE_ANNOTATION_NAME
                    }
                    if (isBaseTT) {
                        baseTransformedTypes += classNameWithParameters
                    }

                    val manual = klass.superTypeListEntries.find {
                        it.typeReference?.findDescendantOfType<KtAnnotationEntry> { annotationEntry ->
                            annotationEntry.shortName?.asString() == VISITED_SUPERTYPE_ANNOTATION_NAME
                        } != null
                    }
                    if (manual != null) {
                        references[classNameWithParameters] = listOfNotNull(manual.toNameWithTypeParameters())
                    } else {
                        references[classNameWithParameters] =
                                klass.superTypeListEntries.mapNotNull {
                                    it.toNameWithTypeParameters()
                                }
                    }
                }

                super.visitClass(klass)
            }
        })
    }


    private fun <K> Map<K, List<K>>.computeBackReferences(): Map<K, List<K>> {
        val result = mutableMapOf<K, List<K>>()

        this.forEach { (k, v) ->
            v.forEach {
                result.merge(it, listOf(k)) { a, b -> a + b }
            }
        }
        return result
    }

    private fun <K : Comparable<K>> Map<K, List<K>>.sortedMap(): Map<K, List<K>> {
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
            cleanBack.computeBackReferences().sortedMap(),
            cleanBack.sortedMap(),
            packagePerClass.filterKeys { it in keysToKeep }.values.distinct().sortedBy { it.asString() },
            baseTransformedTypes.sorted()
        )
    }

    data class ReferencesData(
        val direct: Map<NameWithTypeParameters, List<NameWithTypeParameters>>,
        val back: Map<NameWithTypeParameters, List<NameWithTypeParameters>>,
        val usedPackages: List<FqName>,
        val baseTransformedTypes: List<NameWithTypeParameters>
    )
}