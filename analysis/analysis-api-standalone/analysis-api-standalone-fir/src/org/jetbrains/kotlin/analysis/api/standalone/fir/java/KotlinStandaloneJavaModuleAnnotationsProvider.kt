/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.java

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.fir.java.KaFirJavaAnnotationPsi
import org.jetbrains.kotlin.analysis.api.platform.java.KotlinJavaModulePsiAnnotationsProvider
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver

/**
 * Delegates directly to the compiler's [JavaModuleResolver] as we can use it in Standalone.
 *
 * The resulting [JavaAnnotation]s are not necessarily backed by PSI, so they are passed in [KaFirJavaAnnotationPsi], which
 * `KaFirJavaModuleResolver` unwraps.
 */
@KaImplementationDetail
class KotlinStandaloneJavaModuleAnnotationsProvider(
    project: Project,
    private val javaModuleResolver: JavaModuleResolver,
) : KotlinJavaModulePsiAnnotationsProvider {
    private val psiManager = PsiManager.getInstance(project)

    override fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<PsiAnnotation>? =
        javaModuleResolver.getAnnotationsForModuleOwnerOfClass(classId)?.map { KaFirJavaAnnotationPsi(it, psiManager) }
}
