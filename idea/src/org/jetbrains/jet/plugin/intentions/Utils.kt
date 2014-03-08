package org.jetbrains.jet.plugin.intentions

import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences

fun specifyTypeExplicitly(declaration: JetNamedFunction, typeText: String) {
    specifyTypeExplicitly(declaration, JetPsiFactory.createType(declaration.getProject(), typeText))
}

fun specifyTypeExplicitly(declaration: JetNamedFunction, `type`: JetType) {
    if (`type`.isError()) return
    val typeReference = JetPsiFactory.createType(declaration.getProject(), DescriptorRenderer.SOURCE_CODE.renderType(`type`))
    specifyTypeExplicitly(declaration, typeReference)
    ShortenReferences.process(declaration.getReturnTypeRef()!!)
}

fun specifyTypeExplicitly(declaration: JetNamedFunction, typeReference: JetTypeReference) {
    val project = declaration.getProject()
    val anchor = declaration.getValueParameterList() ?: return/*incomplete declaration*/
    declaration.addAfter(typeReference, anchor)
    declaration.addAfter(JetPsiFactory.createColon(project), anchor)
}

fun expressionType(expression: JetExpression): JetType? {
    val resolveSession = AnalyzerFacadeWithCache.getLazyResolveSessionForFile(expression.getContainingFile() as JetFile)
    val bindingContext = resolveSession.resolveToElement(expression)
    return bindingContext.get(BindingContext.EXPRESSION_TYPE, expression)
}

fun functionReturnType(function: JetNamedFunction): JetType? {
    val resolveSession = AnalyzerFacadeWithCache.getLazyResolveSessionForFile(function.getContainingFile() as JetFile)
    val bindingContext = resolveSession.resolveToElement(function)
    val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, function)
    if (descriptor == null) return null
    return (descriptor as FunctionDescriptor).getReturnType()
}
