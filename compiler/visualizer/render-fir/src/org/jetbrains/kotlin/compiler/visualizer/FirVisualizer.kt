/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.visualizer

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.directExpansionType
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.firUnsafe
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

private typealias Stack = MutableList<Pair<String, MutableList<String>>>

class FirVisualizer(private val firFile: FirFile) : BaseRenderer() {
    private fun FirElement.render(): String = buildString { this@render.accept(FirRenderer(), this) }

    override fun addAnnotation(annotationText: String, element: PsiElement?, deleteDuplicate: Boolean) {
        super.addAnnotation(annotationText, element, false)
    }

    override fun render(): String {
        val map = mutableMapOf<PsiElement, MutableList<FirElement>>().apply { Psi2FirMapper(this).visitFile(firFile) }
        map.keys.firstOrNull { it is KtFile }?.accept(PsiVisitor(map))
        return Annotator.annotate(firFile.psi!!.text, getAnnotations()).joinToString("\n")
    }

    inner class PsiVisitor(private val map: Map<PsiElement, MutableList<FirElement>>) : KtVisitorVoid() {
        //private var innerLambdaCount = 0
        private val stack = mutableListOf("" to mutableListOf<String>())

        private fun Stack.push(
            levelName: String,
            defaultValues: MutableList<String> = mutableListOf()
        ) = this.add(levelName to defaultValues)

        private fun Stack.pop() = this.removeAt(this.size - 1)
        private fun Stack.addName(name: String) = this.last().second.add(name)
        private fun Stack.addName(name: Name) = this.addName(name.asString())
        private fun Stack.getPathByName(name: String): String {
            for ((reversedIndex, names) in this.asReversed().map { it.second }.withIndex()) {
                if (names.contains(name)) {
                    return this.filterIndexed { index, _ -> index < this.size - reversedIndex && index > 0 }
                        .joinToString(separator = ".", postfix = ".") { it.first }
                }
            }
            if (name == "it") {
                return this.subList(1, this.size)
                    .joinToString(separator = ".", postfix = ".") { it.first }
            }
            return "[NOT FOUND]."
        }

        private inline fun <reified T> KtElement.firstOfType(): T? {
            val firList = map[this]
            return firList?.filterIsInstance<T>()?.firstOrNull()
        }

        /**
         * @return rendered element or null if there is no such type
         */
        private inline fun <reified T : FirElement> KtElement.firstOfTypeWithRender(
            psi: PsiElement? = this,
            getRendererElement: T.() -> FirElement = { this }
        ): FirElement? {
            return firstOfType<T>()?.also { addAnnotation(it.getRendererElement().render(), psi) }
        }

        /**
         * @return rendered element or null if there is no such type
         */
        private inline fun <reified T : FirElement> KtElement.firstOfTypeWithLocalReplace(
            psi: PsiElement? = this,
            getName: T.() -> String
        ): FirElement? {
            return firstOfType<T>()?.also { addAnnotation(it.render().replace("<local>/", stack.getPathByName(it.getName())), psi) }
        }

        /**
         * @return first rendered element or null if there is no such type
         */
        private inline fun <reified T : FirElement> KtElement.allOfTypeWithLocalReplace(
            psi: PsiElement? = this,
            getName: T.() -> String
        ): FirElement? {
            val firList = map[this]
            val firElements = firList?.filterIsInstance<T>()
            if (firElements == null || firElements.isEmpty()) return null
            firElements.forEach { addAnnotation(it.render().replace("<local>/", stack.getPathByName(it.getName())), psi) }

            return firElements.first()
        }

        override fun visitElement(element: PsiElement) {
            element.acceptChildren(this)
        }

        override fun visitKtElement(element: KtElement) {
            when (element) {
                is KtClassInitializer, is KtSecondaryConstructor, is KtPrimaryConstructor, is KtSuperTypeCallEntry -> {
                    val valueParameters = element.getChildrenOfType<KtParameterList>()
                    valueParameters.flatMap { it.parameters }.forEach { stack.addName(it.nameAsSafeName) }

                    //add to init values from last block
                    //because when we are out of primary constructor information about properties will be removed
                    //is used in ClassInitializer block and in SuperTypeCallEntry
                    stack.push("<init>", stack.last().second)
                    element.acceptChildren(this)
                    stack.pop()
                }
                is KtClassOrObject -> {
                    stack.push((element.name ?: "<no name provided>"))
                    element.acceptChildren(this)
                    stack.pop()
                }
                else -> element.acceptChildren(this)
            }
        }

        override fun visitPackageDirective(directive: KtPackageDirective) {
            //don't resolve package names
        }

        override fun visitSuperExpression(expression: KtSuperExpression) {
            //don't resolve super expression
        }

        override fun visitThisExpression(expression: KtThisExpression) {
            //don't resolve this expression
        }

        override fun visitNamedFunction(function: KtNamedFunction) {
            stack.push((function.name ?: "<no name provided>"))
            if (function.equalsToken != null) {
                function.bodyExpression!!.firstOfTypeWithRender<FirExpression>(function.equalsToken) { this.typeRef }
                    ?: function.firstOfTypeWithRender<FirTypedDeclaration>(function.equalsToken) { this.returnTypeRef }
            }
            super.visitNamedFunction(function)
            stack.pop()
        }

        private fun renderVariableType(variable: KtVariableDeclaration) {
            stack.addName(variable.nameAsSafeName)
            variable.firstOfTypeWithRender<FirVariable<*>>(variable.nameIdentifier)
            variable.acceptChildren(this)
        }

        override fun visitProperty(property: KtProperty) =
            renderVariableType(property)

        override fun visitDestructuringDeclarationEntry(multiDeclarationEntry: KtDestructuringDeclarationEntry) =
            renderVariableType(multiDeclarationEntry)

        override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
            annotationEntry.firstOfTypeWithRender<FirAnnotationCall>()
            super.visitAnnotationEntry(annotationEntry)
        }

        override fun visitConstructorCalleeExpression(constructorCalleeExpression: KtConstructorCalleeExpression) {
            constructorCalleeExpression.firstOfTypeWithRender<FirDelegatedConstructorCall>()
        }

        override fun visitParameter(parameter: KtParameter) {
            stack.addName(parameter.nameAsSafeName)
            if ((parameter.isLoopParameter && parameter.destructuringDeclaration == null) || parameter.ownerFunction is KtPropertyAccessor) {
                parameter.firstOfTypeWithRender<FirVariable<*>>(parameter.nameIdentifier)
            }
            super.visitParameter(parameter)
        }

        override fun visitTypeReference(typeReference: KtTypeReference) {
            typeReference.firstOfTypeWithRender<FirTypeRef>()
            super.visitTypeReference(typeReference)
        }

        override fun visitConstantExpression(expression: KtConstantExpression) {
            expression.firstOfTypeWithRender<FirConstExpression<*>>()
        }

        override fun visitReferenceExpression(expression: KtReferenceExpression) {
            if (expression is KtOperationReferenceExpression) return

            expression.firstOfTypeWithLocalReplace<FirResolvedNamedReference> { this.name.asString() }
                ?: expression.firstOfTypeWithRender<FirResolvedQualifier>()
                ?: expression.firstOfTypeWithRender<FirElement>() //fallback for errors
            super.visitReferenceExpression(expression)
        }

        override fun visitUnaryExpression(expression: KtUnaryExpression) {
            if (expression.operationReference.getReferencedName() == "!!") {
                expression.baseExpression?.accept(this)
                return
            }
            expression.firstOfTypeWithLocalReplace<FirFunctionCall>(expression.operationReference) { this.calleeReference.name.asString() }
            super.visitUnaryExpression(expression)
        }

        override fun visitBinaryExpression(expression: KtBinaryExpression) {
            if (expression.operationReference.getReferencedName() == "?:") {
                expression.left?.accept(this)
                expression.right?.accept(this)
                return
            }

            expression.operationReference.let {
                expression.allOfTypeWithLocalReplace<FirFunctionCall>(it) { this.calleeReference.name.asString() }
                    ?: expression.firstOfTypeWithLocalReplace<FirVariableAssignment>(it) { this.lValue.toString() }
                    ?: expression.firstOfTypeWithRender<FirOperatorCall>(it)
            }
            super.visitBinaryExpression(expression)
        }

        override fun visitIfExpression(expression: KtIfExpression) {
            expression.firstOfTypeWithRender<FirWhenExpression> { this.typeRef }
            super.visitIfExpression(expression)
        }

        override fun visitWhenExpression(expression: KtWhenExpression) {
            expression.firstOfTypeWithRender<FirWhenExpression> { this.typeRef }
            super.visitWhenExpression(expression)
        }

        override fun visitCallExpression(expression: KtCallExpression) {
            expression.firstOfTypeWithLocalReplace<FirFunctionCall> { this.calleeReference.name.asString() }
            expression.children.filter { it.node.elementType != KtNodeTypes.REFERENCE_EXPRESSION }.forEach { it.accept(this) }
        }

        override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
            val firLabel = lambdaExpression.firstOfType<FirLabel>()

            stack.push("<anonymous>")
            //firLabel?.let { addAnnotation("${it.name}@$innerLambdaCount", lambdaExpression) }
            //innerLambdaCount++
            super.visitLambdaExpression(lambdaExpression)
            //innerLambdaCount--
            stack.pop()
        }

        override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
            //this method explicitly accept children and prevent default fallback to other fir element
            expression.acceptChildren(this)
        }

        override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
            if (accessor.isSetter) {
                stack.push("<set-${accessor.property.nameAsSafeName}>", mutableListOf())
                super.visitPropertyAccessor(accessor)
                stack.pop()
            } else {
                super.visitPropertyAccessor(accessor)
            }
        }

        override fun visitWhenEntry(jetWhenEntry: KtWhenEntry) {
            jetWhenEntry.firstOfTypeWithRender<FirWhenBranch>(jetWhenEntry.expression) { this.result.typeRef }
            super.visitWhenEntry(jetWhenEntry)
        }
    }

    inner class FirRenderer : FirVisitor<Unit, StringBuilder>() {
        private val session = firFile.session
        private val filePackage = firFile.packageFqName.toString().replace(".", "/")
        private val symbolProvider = firFile.session.firSymbolProvider

        private fun removeCurrentFilePackage(fqName: String): String {
            return if (fqName.startsWith(filePackage) && !fqName.substring(filePackage.length + 1).contains("/")) {
                fqName.replaceFirst("$filePackage/", "")
            } else {
                fqName
            }
        }

        private fun ClassId.getWithoutCurrentPackage() = removeCurrentFilePackage(this.asString())

        private fun <T : FirElement> renderListInTriangles(list: List<T>, data: StringBuilder, withSpace: Boolean = false) {
            if (list.isNotEmpty()) {
                list.joinTo(data, separator = ", ", prefix = "<", postfix = ">") {
                    buildString { it.accept(this@FirRenderer, this) }
                }
                if (withSpace) data.append(" ")
            }
        }

        private fun visitArguments(arguments: List<FirExpression>, data: StringBuilder) {
            arguments.joinTo(data, ", ", "(", ")") {
                if (it is FirResolvedQualifier) {
                    val lookupTag = (it.typeRef as FirResolvedTypeRefImpl).coneTypeSafe<ConeClassLikeType>()?.lookupTag
                    val type = lookupTag?.let {
                        (symbolProvider.getSymbolByLookupTag(it)?.fir as? FirClass)?.superTypeRefs?.first()?.render()
                    }
                    if (type != null) return@joinTo type
                }
                it.typeRef.render()
            }
        }

        override fun visitElement(element: FirElement, data: StringBuilder) {
            element.acceptChildren(this, data)
        }

        override fun visitConstructor(constructor: FirConstructor, data: StringBuilder) {
            data.append(renderSymbol(constructor.symbol))
            visitValueParameters(constructor.valueParameters, data)
        }

        override fun visitTypeParameter(typeParameter: FirTypeParameter, data: StringBuilder) {
            data.append(typeParameter.name)
            val bounds = typeParameter.bounds.filterNot { it.render() == "kotlin/Any?" }
            if (bounds.isNotEmpty()) {
                data.append(" : ")
                bounds.joinTo(data, separator = ", ") {
                    buildString { it.accept(this@FirRenderer, this) }
                }
            }
        }

        override fun visitProperty(property: FirProperty, data: StringBuilder) {
            if (property.isLocal) {
                visitVariable(property, data)
                return
            }
            data.append(property.returnTypeRef.render())
        }

        private fun visitValueParameters(valueParameters: List<FirValueParameter>, data: StringBuilder) {
            valueParameters.joinTo(data, separator = ", ", prefix = "(", postfix = ")") {
                buildString { it.accept(this@FirRenderer, this) }
            }
        }

        override fun visitValueParameter(valueParameter: FirValueParameter, data: StringBuilder) {
            if (valueParameter.isVararg) {
                data.append("vararg ")
            }
            valueParameter.returnTypeRef.coneTypeSafe<ConeClassLikeType>()?.arrayElementType(session)?.let { data.append(it.render()) }
                ?: valueParameter.returnTypeRef.accept(this, data)
            valueParameter.defaultValue?.let { data.append(" = ...") }
        }

        override fun <F : FirVariable<F>> visitVariable(variable: FirVariable<F>, data: StringBuilder) {
            data.append(variable.returnTypeRef.render())
        }

        override fun visitNamedReference(namedReference: FirNamedReference, data: StringBuilder) {
            if (namedReference is FirErrorNamedReference) {
                data.append("[ERROR : ${namedReference.diagnostic.reason}]")
                return
            }
            visitElement(namedReference, data)
        }

        override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference, data: StringBuilder) {
            val symbol = resolvedNamedReference.resolvedSymbol
            data.append(renderSymbol(symbol))
        }

        override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: StringBuilder) {
            data.append(annotationCall.annotationTypeRef.render())
        }

        override fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall, data: StringBuilder) {
            val coneClassType = delegatedConstructorCall.constructedTypeRef.coneTypeSafe<ConeClassLikeType>()
            if (coneClassType != null) {
                data.append("constructor").append(" ")

                data.append(coneClassType.lookupTag.classId.getWithoutCurrentPackage())

                val typeParameters =
                    (symbolProvider.getSymbolByLookupTag(coneClassType.lookupTag)?.fir as? FirRegularClass)
                        ?.typeParameters ?: emptyList()
                renderListInTriangles(typeParameters, data)
                visitArguments(delegatedConstructorCall.arguments, data)
            } else {
                data.append("[ERROR : ${delegatedConstructorCall.constructedTypeRef.render()}]")
            }
        }

        override fun visitOperatorCall(operatorCall: FirOperatorCall, data: StringBuilder) {
            data.append("operator call ${operatorCall.operation}")
        }

        override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: StringBuilder) {
            //skip rendering for as/as?/is/!is
        }

        override fun visitFunctionCall(functionCall: FirFunctionCall, data: StringBuilder) {
            when (val callee = functionCall.calleeReference) {
                is FirResolvedNamedReference -> {
                    if (callee.resolvedSymbol is FirConstructorSymbol) {
                        data.append(renderSymbol(callee.resolvedSymbol))
                        visitArguments(functionCall.arguments, data)
                    } else {
                        data.append("fun ")
                        val firFunction = callee.resolvedSymbol.fir as? FirTypeParametersOwner
                        firFunction?.let { renderListInTriangles(it.typeParameters, data, true) }

                        data.append(renderSymbol(callee.resolvedSymbol))
                        renderListInTriangles(functionCall.typeArguments, data)
                        visitValueParameters(callee.resolvedSymbol.firUnsafe<FirFunction<*>>().valueParameters, data)
                        data.append(": ")
                        functionCall.typeRef.accept(this, data)
                    }
                }
                is FirErrorNamedReference -> data.append("[ERROR : ${callee.diagnostic.reason}]")
            }
        }

        override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: StringBuilder) {
            if (constExpression.kind != IrConstKind.String) {
                data.append(constExpression.kind)
            }
        }

        override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: StringBuilder) {
            resolvedQualifier.classId?.let {
                val fir = symbolProvider.getClassLikeSymbolByFqName(it)?.fir
                if (fir is FirClass) {
                    data.append(fir.classKind.name.toLowerCase()).append(" ")
                    data.append((fir as? FirRegularClass)?.name ?: Name.special("<anonymous>"))
                    if (fir.superTypeRefs.any { it.render() != "kotlin/Any" }) {
                        data.append(": ")
                        fir.superTypeRefs.joinTo(data, separator = ", ") { typeRef -> typeRef.render() }
                    }
                }
            }
        }

        override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: StringBuilder) {
            data.append("variable assignment")
        }

        override fun visitStarProjection(starProjection: FirStarProjection, data: StringBuilder) {
            data.append("*")
        }

        override fun visitTypeProjectionWithVariance(typeProjectionWithVariance: FirTypeProjectionWithVariance, data: StringBuilder) {
            val variance = typeProjectionWithVariance.variance.label
            if (variance.isNotEmpty()) data.append("$variance ")
            typeProjectionWithVariance.typeRef.accept(this, data)
        }

        override fun visitDelegatedTypeRef(delegatedTypeRef: FirDelegatedTypeRef, data: StringBuilder) {
            delegatedTypeRef.typeRef.accept(this, data)
            data.append(" by ")
            delegatedTypeRef.delegate?.accept(this, data)
        }

        override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: StringBuilder) {
            val coneType = resolvedTypeRef.type
            data.append(removeCurrentFilePackage(coneType.render()))
            if (coneType is ConeClassLikeType) {
                val original = coneType.directExpansionType(session)
                original?.let { data.append(" /* = ${it.render()} */") }
            }
        }

        override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: StringBuilder) {
            data.append("[ERROR : ${errorTypeRef.diagnostic.reason}]")
        }

        override fun visitResolvedFunctionTypeRef(resolvedFunctionTypeRef: FirResolvedFunctionTypeRef, data: StringBuilder) {
            resolvedFunctionTypeRef.receiverTypeRef?.let {
                it.accept(this, data)
                data.append(".")
            }
            visitValueParameters(resolvedFunctionTypeRef.valueParameters, data)
            data.append(" -> ")
            resolvedFunctionTypeRef.returnTypeRef.accept(this, data)
        }

        override fun visitTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability, data: StringBuilder) {
            if (typeRefWithNullability.isMarkedNullable) {
                data.append("?")
            }
        }

        private fun renderSymbol(symbol: AbstractFirBasedSymbol<*>?): String {
            val data = StringBuilder()
            var id = when (symbol) {
                is FirCallableSymbol<*> -> {
                    val callableId = symbol.callableId
                    val idWithPackage = callableId.toString().replace("." + callableId.callableName.asString(), "")
                    removeCurrentFilePackage(idWithPackage)
                }
                is FirClassLikeSymbol<*> -> symbol.classId.getWithoutCurrentPackage()
                else -> ""
            }

            if (id.startsWith("/")) {
                id = id.substring(1)
            }

            fun renderVariable(variable: FirVariable<*>) {
                if (variable !is FirValueParameter) {
                    if (variable.isVar) data.append("var ") else if (variable.isVal) data.append("val ")
                }
                data.append(id)

                data.append(": ")
                variable.returnTypeRef.accept(this, data)
            }

            when (symbol) {
                is FirNamedFunctionSymbol -> {
                    val callableName = symbol.callableId.callableName
                    val receiverType = symbol.fir.receiverTypeRef
                    if (receiverType == null) {
                        if (symbol.callableId.className == null) {
                            data.append(id)
                        } else {
                            data.append("($id).$callableName")
                        }
                    } else {
                        data.append("${receiverType.render()}.$callableName")
                    }
                }
                is FirPropertySymbol -> {
                    if (symbol.fir.isLocal) {
                        renderVariable(symbol.fir)
                    } else {
                        data.append(if (symbol.fir.isVar) "var" else "val").append(" ")
                        renderListInTriangles(symbol.fir.typeParameters, data, withSpace = true)

                        val receiver = symbol.fir.receiverTypeRef?.render()
                        if (receiver != null) {
                            data.append(receiver).append(".")
                        } else if (id != symbol.callableId.callableName.asString()) {
                            data.append("($id)").append(".")
                        }

                        data.append(symbol.callableId.callableName).append(": ")
                        symbol.fir.returnTypeRef.accept(this, data)
                    }

                }
                is FirVariableSymbol<*> -> {
                    renderVariable(symbol.fir)                }
                is FirConstructorSymbol -> {
                    data.append("constructor ")
                    val packageName = symbol.callableId.className
                    data.append(packageName)
                    renderListInTriangles(symbol.fir.typeParameters, data)
                }
            }
            return data.toString()
        }
    }
}
