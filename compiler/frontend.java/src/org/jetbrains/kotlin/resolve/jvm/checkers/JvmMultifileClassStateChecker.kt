/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

object JvmMultifileClassStateChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is PropertyDescriptor ||
            !DescriptorUtils.isTopLevelDeclaration(descriptor) ||
            descriptor.isExpect ||
            descriptor.isConst) return

        if (!context.languageVersionSettings.getFlag(JvmAnalysisFlags.inheritMultifileParts)) return

        if (!JvmFileClassUtil.getFileClassInfoNoResolve(declaration.containingKtFile).withJvmMultifileClass) return

        if (@Suppress("DEPRECATION") descriptor.isDelegated ||
            context.trace.bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor) == true) {
            context.trace.report(ErrorsJvm.STATE_IN_MULTIFILE_CLASS.on(declaration))
        }
    }
}
