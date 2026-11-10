/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.moduleVisibilityProvider
import org.jetbrains.kotlin.ir.util.ModuleVisibilityProvider

internal object Fir2IrModuleVisibilityProvider : ModuleVisibilityProvider {
    override fun canSeeInternals(thisModule: IrModuleFragment, otherModule: IrModuleFragment): Boolean {
        if (otherModule.moduleVisibilityProvider !is Fir2IrModuleVisibilityProvider) return false

        val thisModuleDesc = thisModule.descriptor as FirModuleDescriptor
        val otherModuleDesc = otherModule.descriptor as FirModuleDescriptor
        if (thisModuleDesc.shouldSeeInternalsOf(otherModuleDesc)) return true

        /*
         * There could be a case when one declaration is located in common sources and another is located in
         * platform dependencies. For the frontend these two module data don't see each other, but from the
         * backend point of view all sources belong to the same module. So in this situation we also should
         * consider if the other declaration is visible from the leaf source module.
         */
        val leafModuleDescriptor = (otherModule.leafModule?.descriptor as FirModuleDescriptor?) ?: return false
        if (thisModuleDesc.session.kind == FirSession.Kind.Source && thisModuleDesc != leafModuleDescriptor) {
            return leafModuleDescriptor.shouldSeeInternalsOf(otherModuleDesc)
        }

        /*
         * This branch could be useful for the case with classpath substitution, if we would make a fake-override
         * for a library class if it has a supertype from source. But at the moment of writing this code it's
         * effectively unreachable, as fake overrides for library classes are built in the `Fir2IrLazyClass.declarations`
         * which doesn't invoke this code. But it was decided to keep it for symmetry.
         */
        if (otherModuleDesc.session.kind == FirSession.Kind.Source && otherModuleDesc != leafModuleDescriptor) {
            return thisModuleDesc.shouldSeeInternalsOf(leafModuleDescriptor)
        }
        return false
    }
}
