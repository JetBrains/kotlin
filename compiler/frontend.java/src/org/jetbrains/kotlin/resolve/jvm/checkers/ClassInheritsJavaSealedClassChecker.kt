/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.bindingContextUtil.getAbbreviatedTypeOrType
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext

object ClassInheritsJavaSealedClassChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor || declaration !is KtClassOrObject) return
        for (superTypeListEntry in declaration.superTypeListEntries) {
            val typeReference = superTypeListEntry.typeReference ?: continue
            val superType = typeReference.getAbbreviatedTypeOrType(context.trace.bindingContext)?.unwrap() ?: continue
            val superClass = superType.constructor.declarationDescriptor as? LazyJavaClassDescriptor ?: continue
            if (superClass.jClass.isSealed) {
                context.trace.report(Errors.CLASS_INHERITS_JAVA_SEALED_CLASS.on(typeReference))
            }
        }
    }
}
