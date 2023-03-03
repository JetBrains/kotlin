/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.backend.common.BackendException
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.resolve.DescriptorUtils

internal class IrSyntheticDeclarationGenerator(context: GeneratorContext) : IrElementVisitorVoid {
    fun generateSyntheticDeclarations(file: IrFile) {
        try {
            file.acceptChildrenVoid(this)
        } catch (e: BackendException) {
            throw e
        } catch (e: Throwable) {
            val psiFile = (file.fileEntry as? PsiIrFileEntry)?.psiFile
            CodegenUtil.reportBackendException(e, "psi2ir", psiFile?.virtualFile?.path ?: psiFile?.name ?: file.fileEntry.name)
        }
    }

    private val descriptorGenerator = SyntheticDeclarationsGenerator(context)
    private val symbolTable = context.symbolTable

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall) {
        expression.acceptChildrenVoid(this)
    }

    private fun collectDescriptors(descriptor: ClassDescriptor): MutableList<DeclarationDescriptor> {
        val result = mutableListOf<DeclarationDescriptor>()
        result.addAll(DescriptorUtils.getAllDescriptors(descriptor.unsubstitutedMemberScope))
        result.addAll(descriptor.constructors)
        descriptor.companionObjectDescriptor?.let { result.add(it) }

        return result
    }

    private fun ensureMemberScope(irClass: IrClass) {
        val declaredDescriptors = irClass.declarations.map { it.descriptor }
        val contributedDescriptors = collectDescriptors(irClass.descriptor)

        contributedDescriptors.removeAll(declaredDescriptors)

        symbolTable.withScope(irClass) {
            contributedDescriptors.forEach { it.accept(descriptorGenerator, irClass) }
        }
    }

    override fun visitClass(declaration: IrClass) {
        ensureMemberScope(declaration)
        declaration.acceptChildrenVoid(this)
    }

}
