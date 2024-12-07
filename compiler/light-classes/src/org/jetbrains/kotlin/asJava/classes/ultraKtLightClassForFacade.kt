/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isPrivate

class KtUltraLightClassForFacade(
    facadeClassFqName: FqName,
    files: Collection<KtFile>,
    private val filesWithSupports: Collection<Pair<KtFile, KtUltraLightSupport>>,
) : KtLightClassForFacadeBase(facadeClassFqName, files) {
    private val _modifierListForSimpleFacade: PsiModifierList by lazyPub {
        KtUltraLightSimpleModifierList(owner = this, modifiers = setOf(PsiModifier.PUBLIC, PsiModifier.FINAL))
    }

    private val _givenAnnotations: List<KtLightAbstractAnnotation>? by lazyPub {
        files.flatMap { file ->
            file.annotationEntries.map { entry ->
                KtLightAnnotationForSourceEntry(
                    name = entry.shortName?.identifier,
                    lazyQualifiedName = { entry.analyzeAnnotation()?.fqName?.asString() },
                    kotlinOrigin = entry,
                    parent = modifierList
                )
            }
        }
    }

    override val givenAnnotations: List<KtLightAbstractAnnotation>?
        get() = if (multiFileClass) emptyList() else _givenAnnotations

    override fun createModifierListForSimpleFacade(): PsiModifierList = _modifierListForSimpleFacade

    override fun getScope(): PsiElement? = parent

    private val filesWithSupportsWithCreators by lazyPub {
        filesWithSupports.map { (file, support) ->
            Triple(
                file,
                support,
                UltraLightMembersCreator(
                    containingClass = this,
                    containingClassIsNamedObject = false,
                    containingClassIsSealed = true,
                    mangleInternalFunctions = false,
                    support = support
                )
            )
        }
    }

    private fun loadMethodsFromFile(
        file: KtFile,
        support: KtUltraLightSupport,
        creator: UltraLightMembersCreator,
        result: MutableList<KtLightMethod>
    ) {
        for (declaration in file.declarations.filterNot { it.isHiddenByDeprecation(support) }) {
            val methods = when (declaration) {
                is KtNamedFunction -> creator.createMethods(
                    ktFunction = declaration,
                    forceStatic = true
                )

                is KtProperty -> {
                    if (!declaration.isPrivate() || declaration.accessors.isNotEmpty()) {
                        creator.propertyAccessors(
                            declaration = declaration,
                            mutable = declaration.isVar,
                            forceStatic = true,
                            onlyJvmStatic = false,
                        )
                    } else emptyList()
                }

                else -> emptyList()
            }
            result.addAll(methods)
        }
    }

    override fun createOwnFields(): List<KtLightField> = hashSetOf<String>().let { nameCache ->
        filesWithSupportsWithCreators.flatMap { (file, _, creator) ->
            val allProperties = file.declarations.filterIsInstance<KtProperty>()
            val properties = if (multiFileClass) allProperties.filter { it.hasModifier(KtTokens.CONST_KEYWORD) } else allProperties
            properties.mapNotNull {
                creator.createPropertyField(it, nameCache, forceStatic = true)
            }
        }
    }

    override fun createOwnMethods() = mutableListOf<KtLightMethod>().let { result ->
        for ((file, support, creator) in filesWithSupportsWithCreators) {
            loadMethodsFromFile(file, support, creator, result)
        }

        if (!multiFileClass) result else result.filterNot { it.hasModifierProperty(PsiModifier.PRIVATE) }
    }

    override fun copy(): KtLightClassForFacade = KtUltraLightClassForFacade(facadeClassFqName, files, filesWithSupports)
}
