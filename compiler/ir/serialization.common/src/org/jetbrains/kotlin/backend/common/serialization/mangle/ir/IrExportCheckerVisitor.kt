/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle.ir

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.SpecialDeclarationType
import org.jetbrains.kotlin.backend.common.serialization.mangle.publishedApiAnnotation
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

abstract class IrExportCheckerVisitor(private val compatibleMode: Boolean) : KotlinExportChecker<IrDeclaration> {

    private val compatibleChecker = CompatibleChecker()
    private val checker = Checker()

    /**
     * @return true if [declaration] is exportable from klib point of view.
     * Depending on [compatibleMode] option the same declaration could have FileLocal or Common signature.
     */
    override fun check(declaration: IrDeclaration, type: SpecialDeclarationType): Boolean {
        return declaration.accept(if (compatibleMode) compatibleChecker else checker, null)
    }

    abstract override fun IrDeclaration.isPlatformSpecificExported(): Boolean

    /**
     * Corresponding to export policy of klib ABI >= 1.6.0.
     * In that case any non-local declaration (including type parameter and field) is exportable and could be navigated between modules
     */
    private class Checker : IrElementVisitor<Boolean, Nothing?> {
        override fun visitElement(element: IrElement, data: Nothing?): Boolean {
            error("Should bot reach here ${element.render()}")
        }

        override fun visitDeclaration(declaration: IrDeclarationBase, data: Nothing?): Boolean {
            val visibility = (declaration as? IrDeclarationWithVisibility)?.visibility

            if (visibility == DescriptorVisibilities.LOCAL)
                return false

            return declaration.parent.accept(this, data)
        }

        override fun visitClass(declaration: IrClass, data: Nothing?): Boolean {
            if (declaration.name.isAnonymous) return false
            return super.visitClass(declaration, data)
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?): Boolean {
            if (declaration.name.isAnonymous) return false
            return super.visitSimpleFunction(declaration, data)
        }

        override fun visitPackageFragment(declaration: IrPackageFragment, data: Nothing?): Boolean = true

        override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?): Boolean = false

        override fun visitVariable(declaration: IrVariable, data: Nothing?): Boolean = false

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Nothing?): Boolean = false

        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?): Boolean = false

        override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: Nothing?): Boolean = false
    }

    /**
     * Was using for klib ABI version <= 1.5.0. In that case declaration which has itself or in their hierarchy private or local parent
     * is considered non-exportable.
     *
     * Also type parameters and fields are not exportable.
     *
     * Is used to link libraries with ABI level <= 1.5.0
     */
    private inner class CompatibleChecker : IrElementVisitor<Boolean, Nothing?> {
        private fun IrDeclaration.isExported(annotations: List<IrConstructorCall>, visibility: DescriptorVisibility?): Boolean {
            val speciallyExported = annotations.hasAnnotation(publishedApiAnnotation) || isPlatformSpecificExported()

            val selfExported = speciallyExported || visibility == null || visibility.isPubliclyVisible()

            return selfExported && parent.accept(this@CompatibleChecker, null)
        }

        private fun DescriptorVisibility.isPubliclyVisible(): Boolean = isPublicAPI || this === DescriptorVisibilities.INTERNAL

        override fun visitElement(element: IrElement, data: Nothing?): Boolean = error("Should bot reach here ${element.render()}")

        override fun visitDeclaration(declaration: IrDeclarationBase, data: Nothing?) = declaration.run { isExported(annotations, null) }

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: Nothing?) = false
        override fun visitValueParameter(declaration: IrValueParameter, data: Nothing?) = false
        override fun visitVariable(declaration: IrVariable, data: Nothing?) = false
        override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?) = false
        override fun visitErrorDeclaration(declaration: IrErrorDeclaration, data: Nothing?): Boolean = false

        override fun visitTypeParameter(declaration: IrTypeParameter, data: Nothing?) = false

        override fun visitField(declaration: IrField, data: Nothing?) = false

        override fun visitProperty(declaration: IrProperty, data: Nothing?): Boolean {
            return declaration.run { isExported(annotations, visibility) }
        }

        override fun visitPackageFragment(declaration: IrPackageFragment, data: Nothing?): Boolean = true

        override fun visitTypeAlias(declaration: IrTypeAlias, data: Nothing?): Boolean =
            if (declaration.parent is IrPackageFragment) true
            else declaration.run { isExported(annotations, visibility) }

        override fun visitClass(declaration: IrClass, data: Nothing?): Boolean {
            if (declaration.name == SpecialNames.NO_NAME_PROVIDED) return false
            return declaration.run { isExported(annotations, visibility) }
        }

        override fun visitConstructor(declaration: IrConstructor, data: Nothing?): Boolean {
            val klass = declaration.constructedClass
            return if (klass.kind.isSingleton) klass.accept(this, null) else declaration.run { isExported(annotations, visibility) }
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Nothing?): Boolean {
            val annotations = declaration.run { correspondingPropertySymbol?.owner?.annotations ?: annotations }
            return declaration.run { isExported(annotations, visibility) }
        }
    }
}

val Name.isAnonymous: Boolean
    get() = isSpecial && (this == SpecialNames.ANONYMOUS || this == SpecialNames.NO_NAME_PROVIDED)