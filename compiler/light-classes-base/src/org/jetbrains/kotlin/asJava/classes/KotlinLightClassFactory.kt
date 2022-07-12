/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript

interface KotlinLightClassFactory {
    fun createClass(classOrObject: KtClassOrObject): KtLightClassForSourceDeclaration?
    fun createFacade(project: Project, facadeClassFqName: FqName, searchScope: GlobalSearchScope): KtLightClassForFacade?
    fun createFacadeForSyntheticFile(facadeClassFqName: FqName, file: KtFile): KtLightClassForFacade
    fun createScript(script: KtScript): KtLightClassForScript?

    companion object {
        private val instance: KotlinLightClassFactory
            get() = ServiceManager.getService(KotlinLightClassFactory::class.java)

        fun createClass(classOrObject: KtClassOrObject): KtLightClassForSourceDeclaration? {
            return instance.createClass(classOrObject)
        }

        fun createFacade(project: Project, facadeClassFqName: FqName, searchScope: GlobalSearchScope): KtLightClassForFacade? {
            return instance.createFacade(project, facadeClassFqName, searchScope)
        }

        fun createFacadeForSyntheticFile(facadeClassFqName: FqName, file: KtFile): KtLightClassForFacade {
            return instance.createFacadeForSyntheticFile(facadeClassFqName, file)
        }

        fun createScript(script: KtScript): KtLightClassForScript? {
            return instance.createScript(script)
        }
    }
}