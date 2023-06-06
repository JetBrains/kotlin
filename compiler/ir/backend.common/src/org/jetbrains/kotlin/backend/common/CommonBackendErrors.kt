/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.BackendDiagnosticRenderers.INCOMPATIBILITY
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error2
import org.jetbrains.kotlin.diagnostics.error3
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers.STRING
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.diagnostics.rendering.Renderers.MODULE_WITH_PLATFORM
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility

object CommonBackendErrors {
    val NO_ACTUAL_FOR_EXPECT by error2<PsiElement, String, ModuleDescriptor>()
    val MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED by error2<PsiElement, String, String>()
    val MANY_IMPL_MEMBER_NOT_IMPLEMENTED by error2<PsiElement, String, String>()
    val INCOMPATIBLE_MATCHING by error3<PsiElement, String, String, ExpectActualCompatibility.Incompatible<*>>()

    init {
        RootDiagnosticRendererFactory.registerFactory(KtDefaultCommonBackendErrorMessages)
    }
}

object KtDefaultCommonBackendErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("KT").also { map ->
        map.put(
            CommonBackendErrors.NO_ACTUAL_FOR_EXPECT,
            "Expected {0} has no actual declaration in module {1}",
            STRING,
            MODULE_WITH_PLATFORM,
        )
        map.put(
            CommonBackendErrors.MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED,
            "{0} must override {1} because it inherits multiple interface methods of it",
            STRING,
            STRING,
        )
        map.put(
            CommonBackendErrors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED,
            "{0} must override {1} because it inherits many implementations of it",
            STRING,
            STRING,
        )
        map.put(
            CommonBackendErrors.INCOMPATIBLE_MATCHING,
            "Expect declaration `{0}` doesn''t match actual `{1}` because {2}",
            STRING,
            STRING,
            INCOMPATIBILITY
        )
    }
}

object BackendDiagnosticRenderers {
    val INCOMPATIBILITY = Renderer<ExpectActualCompatibility.Incompatible<*>> {
        it.reason ?: "<unknown>"
    }
}
