package org.jetbrains.kotlin.psi2ir.extensions

import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.StatementGenerator

interface SyntheticIrExtension {
    companion object : ProjectExtensionDescriptor<SyntheticIrExtension>(
            "org.jetbrains.kotlin.syntheticIrExtension", SyntheticIrExtension::class.java)

    fun interceptModuleFragment(context: GeneratorContext, ktFiles:Collection<KtFile>, irModuleFragment: IrModuleFragment) {}
    fun visitCallExpression(statementGenerator: StatementGenerator, element: KtCallExpression): IrExpression? = null
    fun visitSimpleNameExpression(statementGenerator: StatementGenerator, element: KtSimpleNameExpression): IrExpression? = null
}
