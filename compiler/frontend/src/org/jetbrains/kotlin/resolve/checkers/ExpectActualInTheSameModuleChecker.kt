/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.resolve.multiplatform.isCompatibleOrWeaklyIncompatible
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement

object ExpectActualInTheSameModuleChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) return
        if (descriptor !is MemberDescriptor) return
        if (declaration !is KtNamedDeclaration) return
        if (!descriptor.isExpect) return
        // Only look for top level actual members; class members will be handled as a part of that expected class
        if (descriptor.containingDeclaration !is PackageFragmentDescriptor) return
        val module = descriptor.module
        val actuals = ExpectedActualResolver.findActualForExpected(descriptor, module)
            ?.filter { (compatibility, _) -> compatibility.isCompatibleOrWeaklyIncompatible }
            ?.flatMap { (_, members) -> members }
            ?.takeIf(List<MemberDescriptor>::isNotEmpty) ?: return

        // There are 4 cases:
        // 1. `expect` in common module, `actual` in platform module. It's a legal situation. In K2MetadataCompiler compiler, the
        //    declarations have different modules. In backend specific compiler, `expect` declaration is in common module so warning won't
        //    be issued
        // 2. `expect` in common module X, `actual` in common module Y (Y depends on X). It's a legal situation. In K2MetadataCompiler
        //    compiler, the declarations have different modules. In backend specific compiler, `expect` declaration is in common so warning
        //    won't be issued
        // 3. `expect` in common module X, `actual` in the very same common module X. It's an illegal situation. In K2MetadataCompiler
        //    compiler, the declarations have the same module, so the warning will be issue. In backend specific compiler, `expect`
        //    declaration is in common so warning won't be issued
        // 4. `expect` in platform module X, `actual` in the very same platform module X. It's an illegal situation. K2MetadataCompiler is
        //    invoked only for common modules, so it won't issue a warning. In backend specific compiler, `expect` declaration is in
        //    platform module, the warning will be issued
        //
        // The checker doesn't consider cases when platform module depend on another platform module.
        if (module.platform.isCommon()) { /* If true then we are in K2MetadataCompiler. `isCommon` never returns `true` in backend specific
                                             compilers (even when the source is truly common) */
            // modules can be distinguished only in K2MetadataCompiler because in platform specific compiler all sources are put in
            // a single module
            if (actuals.all { it.module != module }) return
        } else { // backend specific compiler
            if (declaration.containingKtFile.isCommonSource == true) return
        }

        context.trace.report(Errors.EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE.on(declaration, descriptor))
        for (actual in actuals) {
            val actualSource = actual.declarationSource ?: continue
            context.trace.report(Errors.EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE.on(actualSource, descriptor))
        }
    }

    private val MemberDescriptor.declarationSource: KtNamedDeclaration?
        get() = (this.source as? KotlinSourceElement)?.psi as? KtNamedDeclaration
}
