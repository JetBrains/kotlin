/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.KotlinExtraDiagnosticsProvider
import org.jetbrains.kotlin.asJava.builder.InvalidLightClassDataHolder
import org.jetbrains.kotlin.asJava.builder.LightClassDataProviderForFileFacade
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics

class CliExtraDiagnosticsProvider : KotlinExtraDiagnosticsProvider {
    override fun forClassOrObject(kclass: KtClassOrObject): Diagnostics {
        val lightClassDataHolder = KtLightClassForSourceDeclaration.getLightClassDataHolder(kclass)
        if (lightClassDataHolder is InvalidLightClassDataHolder) {
            return Diagnostics.EMPTY
        }

        return lightClassDataHolder.extraDiagnostics
    }

    override fun forFacade(file: KtFile, moduleScope: GlobalSearchScope): Diagnostics {
        val project = file.project
        val facadeFqName = JvmFileClassUtil.getFileClassInfoNoResolve(file).facadeClassFqName
        return LightClassDataProviderForFileFacade(project, facadeFqName, moduleScope)
            .compute()
            ?.value
            ?.extraDiagnostics
            ?: Diagnostics.EMPTY
    }
}