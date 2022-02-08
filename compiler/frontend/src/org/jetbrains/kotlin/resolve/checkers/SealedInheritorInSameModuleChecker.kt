/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.isSealed
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.bindingContextUtil.getAbbreviatedTypeOrType
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.resolve.source.PsiSourceFile

object SealedInheritorInSameModuleChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor || declaration !is KtClassOrObject) return
        val currentModule = descriptor.module
        for (superTypeListEntry in declaration.superTypeListEntries) {
            val typeReference = superTypeListEntry.typeReference ?: continue
            val superType = typeReference.getAbbreviatedTypeOrType(context.trace.bindingContext)?.unwrap() ?: continue
            val superClass = superType.constructor.declarationDescriptor ?: continue
            if (superClass.isSealed()) {
                /*
                 * If this condition is true then we compile code from CLI mode and class came from
                 *   common source set (in CLI mode we have single ModuleDescriptor for all MPP modules)
                 *
                 * We don't want to analyze classes from common modules, because there are problems with determining
                 *   relation between module of class and module of super class (it's allowed to declare sealed
                 *   inheritors between `expect` class declaration and its actual), so we assume that incorrect
                 *   declarations will be reported while compiling common module to get metadata
                 *
                 *   common               expect sealed class Base
                 *     |                  class A : Base() // OK, A in same module with Base
                 *     |
                 * jvm-js-native          class B : Base() // OK, B inherits `expect` class, not `actual`
                 *     |
                 *   jvm-js               actual sealed class Base
                 *     |                  class C : Base() // OK, C in same module with actual Base
                 *     |
                 *   jvm-js-2             class D : Base() // Error, D not in same module with actual Base
                 *     |
                 *    jvm                 class E : Base() // Error, E not in same module with actual Base
                 *
                 * In this hierarchy error on `D` will be reported in process of compiling `jvm-js-2` module in metadata mode
                 *   (so `jvm-js-2` assumed as "platform" module and others are "common") and won't be reported during
                 *   compilation of `jvm`.
                 *
                 * There is one problem with this condition: if modules hierarchy is a bamboo (no modules with two children)
                 *   then we won't compile intermediate modules in metadata mode, only the last one, so error on class `D`
                 *   won't be reported. See KT-46031
                 */
                if (descriptor.isFromCommonSource && superClass.module == currentModule) {
                    return
                }
                /*
                 * It's allowed to declare inheritors of expect sealed class in any dependent module until actual
                 *   counterpart for this class will be declared. So if super class is resolved to expect sealed
                 *   class then its allowed to declare inheritor
                 */
                if (!(superClass.isExpect || (superClass.module == currentModule && !superClass.isFromCommonSource))) {
                    context.trace.report(Errors.SEALED_INHERITOR_IN_DIFFERENT_MODULE.on(typeReference))
                }
            }
        }
    }

    private val ClassifierDescriptor.isFromCommonSource: Boolean
        get() = ((this.source.containingFile as? PsiSourceFile)?.psiFile as? KtFile)?.isCommonSource ?: false
}
