/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.multiplatform.findCompatibleExpectsForActual
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice

/**
 * [referenceUndiscoveredExpectSymbols] ensures that `actual` symbols declared in any of the given [files] have an associated `expect`
 * symbol in the symbol table. During lowering, an `expect` symbol may be referenced from an `actual` symbol via _descriptors_, so unbound
 * symbols may occur if the `expect` symbol isn't otherwise included in the symbol table and stubbed before lowering.
 *
 * Undiscovered `expect` symbols are not normally an issue when the source code containing the `expect` declarations is contained in the
 * files to compile, but in cases such as the IDE bytecode tool window, such a source file won't be included in [files].
 */
internal fun SymbolTable.referenceUndiscoveredExpectSymbols(files: Collection<KtFile>, bindingContext: BindingContext) {
    val visitor = UndiscoveredExpectVisitor(this, bindingContext)
    files.forEach(visitor::visitKtFile)
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
class UndiscoveredExpectVisitor(
    private val symbolTable: SymbolTable,
    private val bindingContext: BindingContext,
) : KtTreeVisitorVoid() {
    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        super.visitClassOrObject(classOrObject)
        symbolTable.referenceClass(classOrObject.findExpectForActual(BindingContext.CLASS) ?: return)
    }

    override fun visitTypeAlias(typeAlias: KtTypeAlias) {
        super.visitTypeAlias(typeAlias)
        symbolTable.referenceTypeAlias(typeAlias.findExpectForActual(BindingContext.TYPE_ALIAS) ?: return)
    }

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
        super.visitPrimaryConstructor(constructor)
        referenceConstructor(constructor)
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
        super.visitSecondaryConstructor(constructor)
        referenceConstructor(constructor)
    }

    private fun <T : KtConstructor<T>> referenceConstructor(constructor: KtConstructor<T>) {
        symbolTable.referenceFunction(constructor.findExpectForActual(BindingContext.CONSTRUCTOR) ?: return)
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        symbolTable.referenceFunction(function.findExpectForActual(BindingContext.FUNCTION) ?: return)
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        symbolTable.referenceProperty(
            property.findExpectForActualOfType<PropertyDescriptor, _, _>(BindingContext.VARIABLE) ?: return
        )
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
        super.visitPropertyAccessor(accessor)
        symbolTable.referenceFunction(accessor.findExpectForActual(BindingContext.PROPERTY_ACCESSOR) ?: return)
    }

    private inline fun <K : PsiElement, reified V : MemberDescriptor> K.findExpectForActual(
        bindingContextKey: ReadOnlySlice<K, V>,
    ): V? = findExpectForActualOfType<V, K, V>(bindingContextKey)

    private inline fun <reified D : MemberDescriptor, K : PsiElement, V> K.findExpectForActualOfType(
        bindingContextKey: ReadOnlySlice<K, V>,
    ): D? {
        val descriptor = (bindingContext[bindingContextKey, this] as? D)?.takeIf { it.isActual } ?: return null
        return descriptor.findCompatibleExpectsForActual().singleOrNull() as? D
    }
}
