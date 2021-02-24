/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.util.CachedValue
import org.jetbrains.kotlin.asJava.builder.LightClassData
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolder
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil.JVM_MULTIFILE_CLASS_SHORT
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil.findAnnotationEntryOnFileNoResolve
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isPrivate

class KtUltraLightClassForFacade(
    manager: PsiManager,
    facadeClassFqName: FqName,
    lightClassDataCache: CachedValue<LightClassDataHolder.ForFacade>,
    files: Collection<KtFile>,
    private val filesWithSupports: Collection<Pair<KtFile, KtUltraLightSupport>>
) : KtLightClassForFacade(manager, facadeClassFqName, lightClassDataCache, files) {

    override fun getDelegate(): PsiClass = invalidAccess()

    override val lightClassDataCache: CachedValue<LightClassDataHolder.ForFacade> get() = invalidAccess()

    override val clsDelegate: PsiClass get() = invalidAccess()

    override val lightClassData: LightClassData get() = invalidAccess()

    override val javaFileStub: PsiJavaFileStub? = null

    private val _modifierList: PsiModifierList by lazyPub {
        if (isMultiFileClass)
            LightModifierList(manager, KotlinLanguage.INSTANCE, PsiModifier.PUBLIC, PsiModifier.FINAL)
        else
            KtUltraLightSimpleModifierList(owner = this, modifiers = setOf(PsiModifier.PUBLIC, PsiModifier.FINAL))
    }

    private val isMultiFileClass: Boolean by lazyPub {
        files.size > 1 || files.any { findAnnotationEntryOnFileNoResolve(it, JVM_MULTIFILE_CLASS_SHORT) != null }
    }

    private val _givenAnnotations: List<KtLightAbstractAnnotation>? by lazyPub {
        files.flatMap { file ->
            file.annotationEntries.map { entry ->
                KtLightAnnotationForSourceEntry(
                    name = entry.shortName?.identifier,
                    lazyQualifiedName = { entry.analyzeAnnotation()?.fqName?.asString() },
                    kotlinOrigin = entry,
                    parent = _modifierList,
                    lazyClsDelegate = null
                )
            }
        }
    }

    override val givenAnnotations: List<KtLightAbstractAnnotation>?
        get() = if (isMultiFileClass) emptyList() else _givenAnnotations

    override fun getModifierList(): PsiModifierList = _modifierList

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

    private val _ownMethods: List<KtLightMethod> by lazyPub {
        val result = mutableListOf<KtLightMethod>()
        for ((file, support, creator) in filesWithSupportsWithCreators) {
            loadMethodsFromFile(file, support, creator, result)
        }
        if (!multiFileClass) result else result.filterNot { it.hasModifierProperty(PsiModifier.PRIVATE) }
    }

    private val multiFileClass: Boolean by lazyPub {
        filesWithSupports.any {
            it.second.findAnnotation(it.first, JvmFileClassUtil.JVM_MULTIFILE_CLASS) != null
        }
    }

    private val _ownFields: List<KtLightField> by lazyPub {
        hashSetOf<String>().let { nameCache ->
            filesWithSupportsWithCreators.flatMap { (file, _, creator) ->
                val allProperties = file.declarations.filterIsInstance<KtProperty>()
                val properties = if (multiFileClass) allProperties.filter { it.hasModifier(KtTokens.CONST_KEYWORD) } else allProperties
                properties.mapNotNull {
                    creator.createPropertyField(it, nameCache, forceStatic = true)
                }
            }
        }
    }

    override fun getOwnFields() = _ownFields

    override fun getOwnMethods() = _ownMethods

    override fun copy(): KtLightClassForFacade =
        KtUltraLightClassForFacade(manager, facadeClassFqName, lightClassDataCache, files, filesWithSupports)
}