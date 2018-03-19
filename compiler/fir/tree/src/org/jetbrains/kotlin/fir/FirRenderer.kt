/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.Printer

class FirRenderer(builder: StringBuilder) : FirVisitorVoid() {
    private val printer = Printer(builder)

    private var lineBeginning = true

    private fun print(vararg objects: Any) {
        if (lineBeginning) {
            lineBeginning = false
            printer.print(*objects)
        } else {
            printer.printWithNoIndent(*objects)
        }
    }

    private fun println(vararg objects: Any) {
        print(*objects)
        printer.printlnWithNoIndent()
        lineBeginning = true
    }

    private fun pushIndent() {
        printer.pushIndent()
    }

    private fun popIndent() {
        printer.popIndent()
    }

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitFile(file: FirFile) {
        println("FILE: ${file.name}")
        pushIndent()
        super.visitFile(file)
        popIndent()
    }

    private fun List<FirElement>.renderSeparated() {
        for ((index, element) in this.withIndex()) {
            if (index > 0) {
                print(", ")
            }
            element.accept(this@FirRenderer)
        }
    }

    private fun List<FirValueParameter>.renderParameters() {
        print("(")
        renderSeparated()
        print(")")
    }

    private fun List<FirAnnotationCall>.renderAnnotations() {
        for (annotation in this) {
            visitAnnotationCall(annotation)
        }
    }

    private fun Variance.renderVariance() {
        label.let {
            print(it)
            if (it.isNotEmpty()) {
                print(" ")
            }
        }
    }

    override fun visitCallableMember(callableMember: FirCallableMember) {
        visitMemberDeclaration(callableMember)
        val receiverType = callableMember.receiverType
        if (receiverType != null) {
            receiverType.accept(this)
            print(".")
        }
        if (callableMember is FirFunction) {
            callableMember.valueParameters.renderParameters()
        } else if (callableMember is FirProperty) {
            print(if (callableMember.isVar) "(var)" else "(val)")
        }
        print(": ")
        callableMember.returnType.accept(this)
    }

    override fun visitMemberDeclaration(memberDeclaration: FirMemberDeclaration) {
        memberDeclaration.annotations.renderAnnotations()
        if (memberDeclaration.typeParameters.isNotEmpty()) {
            print("<")
            memberDeclaration.typeParameters.renderSeparated()
            print("> ")
        }
        print(memberDeclaration.visibility.toString() + " " + memberDeclaration.modality.name.toLowerCase() + " ")
        if (memberDeclaration is FirCallableMember && memberDeclaration.isOverride) {
            print("override ")
        }
        if (memberDeclaration is FirNamedFunction) {
            if (memberDeclaration.isOperator) {
                print("operator ")
            }
            if (memberDeclaration.isInfix) {
                print("infix ")
            }
            if (memberDeclaration.isInline) {
                print("inline ")
            }
        } else if (memberDeclaration is FirProperty) {
            if (memberDeclaration.isConst) {
                print("const ")
            }
        }

        visitNamedDeclaration(memberDeclaration)
    }

    override fun visitNamedDeclaration(namedDeclaration: FirNamedDeclaration) {
        visitDeclaration(namedDeclaration)
        print(" " + namedDeclaration.name)
    }

    override fun visitDeclaration(declaration: FirDeclaration) {
        print(
            when (declaration) {
                is FirClass -> declaration.classKind.name.toLowerCase().replace("_", " ")
                is FirTypeAlias -> "typealias"
                is FirNamedFunction -> "function"
                is FirProperty -> "property"
                else -> "unknown"
            }
        )
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry) {
        visitClass(enumEntry)
    }

    override fun visitClass(klass: FirClass) {
        visitMemberDeclaration(klass)
        val attributes = listOfNotNull(
            "inner".takeIf { klass.isInner },
            "companion".takeIf { klass.isCompanion },
            "data".takeIf { klass.isData }
        )
        print(attributes.joinToString(prefix = "(", postfix = ")"))
        if (klass.superTypes.isNotEmpty()) {
            print(" : ")
            klass.superTypes.renderSeparated()
        }
        println(" {")
        pushIndent()
        for (declaration in klass.declarations) {
            declaration.accept(this)
            println()
        }
        popIndent()
        println("}")
    }

    override fun visitProperty(property: FirProperty) {
        visitCallableMember(property)
        property.initializer?.let {
            print(" = ")
            it.accept(this)
        }
        pushIndent()
        property.delegate?.let {
            print("by ")
            it.accept(this)
        }
        println()
        property.getter.accept(this)
        if (property.getter.body == null) {
            println()
        }
        if (property.isVar) {
            property.setter.accept(this)
            if (property.setter.body == null) {
                println()
            }
        }
        popIndent()
    }

    override fun visitNamedFunction(namedFunction: FirNamedFunction) {
        visitCallableMember(namedFunction)
        namedFunction.body?.accept(this)
        if (namedFunction.body == null) {
            println()
        }
    }

    override fun visitConstructor(constructor: FirConstructor) {
        constructor.annotations.renderAnnotations()
        print(constructor.visibility.toString() + " constructor")
        constructor.valueParameters.renderParameters()
        constructor.delegatedConstructor?.accept(this)
        constructor.body?.accept(this)
        if (constructor.body == null) {
            println()
        }
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
        propertyAccessor.annotations.renderAnnotations()
        print(propertyAccessor.visibility.toString() + " ")
        print(if (propertyAccessor.isGetter) "get" else "set")
        propertyAccessor.valueParameters.renderParameters()
        print(": ")
        propertyAccessor.returnType.accept(this)
        propertyAccessor.body?.accept(this)
    }

    override fun visitFunction(function: FirFunction) {
        function.valueParameters.renderParameters()
        visitDeclarationWithBody(function)
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) {
        print("init")
        anonymousInitializer.body?.accept(this)
    }

    override fun visitDeclarationWithBody(declarationWithBody: FirDeclarationWithBody) {
        visitDeclaration(declarationWithBody)
        declarationWithBody.body?.accept(this)
    }

    override fun visitBody(body: FirBody) {
        println(" {")
        pushIndent()
        for (statement in body.statements) {
            statement.accept(this)
            println()
        }
        popIndent()
        println("}")
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias) {
        typeAlias.annotations.renderAnnotations()
        visitMemberDeclaration(typeAlias)
        print(" = ")
        typeAlias.abbreviatedType.accept(this)
        println()
    }

    override fun visitTypeParameter(typeParameter: FirTypeParameter) {
        typeParameter.annotations.renderAnnotations()
        typeParameter.variance.renderVariance()
        print(typeParameter.name)
        if (typeParameter.bounds.isNotEmpty()) {
            print(" : ")
            typeParameter.bounds.renderSeparated()
        }
    }

    override fun visitTypedDeclaration(typedDeclaration: FirTypedDeclaration) {
        visitDeclaration(typedDeclaration)
    }

    override fun visitValueParameter(valueParameter: FirValueParameter) {
        valueParameter.annotations.renderAnnotations()
        if (valueParameter.isCrossinline) {
            print("crossinline ")
        }
        if (valueParameter.isNoinline) {
            print("noinline ")
        }
        if (valueParameter.isVararg) {
            print("vararg ")
        }
        print(valueParameter.name.toString() + ": ")
        valueParameter.returnType.accept(this)
        valueParameter.defaultValue?.let {
            print(" = ")
            it.accept(this)
        }
    }

    override fun visitVariable(variable: FirVariable) {
        visitDeclaration(variable)
    }

    override fun visitImport(import: FirImport) {
        visitElement(import)
    }

    override fun visitStatement(statement: FirStatement) {
        visitElement(statement)
    }

    override fun visitExpression(expression: FirExpression) {
        print("STUB")
    }

    override fun visitCall(call: FirCall) {
        visitExpression(call)
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall) {
        visitCall(annotationCall)
    }

    override fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall) {
        visitCall(delegatedConstructorCall)
    }

    override fun visitType(type: FirType) {
        type.annotations.renderAnnotations()
        visitElement(type)
    }

    override fun visitDelegatedType(delegatedType: FirDelegatedType) {
        delegatedType.accept(this)
        print(" by ")
        delegatedType.delegate?.accept(this)
    }

    override fun visitErrorType(errorType: FirErrorType) {
        visitType(errorType)
        print("<ERROR TYPE>")
    }

    override fun visitImplicitType(implicitType: FirImplicitType) {
        print("<implicit>")
    }

    override fun visitTypeWithNullability(typeWithNullability: FirTypeWithNullability) {
        if (typeWithNullability.isNullable) {
            print("?")
        }
    }

    override fun visitDynamicType(dynamicType: FirDynamicType) {
        dynamicType.annotations.renderAnnotations()
        print("<dynamic>")
        visitTypeWithNullability(dynamicType)
    }

    override fun visitFunctionType(functionType: FirFunctionType) {
        print("( ")
        functionType.receiverType?.let {
            it.accept(this)
            print(".")
        }
        functionType.valueParameters.renderParameters()
        print(": ")
        functionType.returnType.accept(this)
        print(" )")
        visitTypeWithNullability(functionType)
    }

    private fun ConeKotlinType.asString(): String {
        return when (this) {
            is ConeKotlinErrorType -> "error: $reason"
            is ConeClassType -> {
                val sb = StringBuilder()
                val fqName = fqName
                val packageFqName = fqName.packageFqName
                if (packageFqName.isRoot) {
                    sb.append("<root>")
                } else {
                    sb.append(packageFqName.asString().replace('.', '/'))
                }
                sb.append('.')
                sb.append(fqName.classFqName.asString())
                sb.append(typeArguments.joinToString { it ->
                    when (it) {
                        StarProjection -> "*"
                        is ConeKotlinTypeProjectionIn -> "in ${it.type.asString()}"
                        is ConeKotlinTypeProjectionOut -> "out ${it.type.asString()}"
                        is ConeKotlinType -> it.asString()
                    }
                })
                sb.toString()
            }
            else -> "Unsupported: $this"
        }
    }

    override fun visitResolvedType(resolvedType: FirResolvedType) {
        resolvedType.annotations.renderAnnotations()
        print("R/")
        val coneType = resolvedType.type
        print(coneType.asString())
        print("/")
        visitTypeWithNullability(resolvedType)
    }

    override fun visitUserType(userType: FirUserType) {
        userType.annotations.renderAnnotations()
        for ((index, qualifier) in userType.qualifier.withIndex()) {
            if (index != 0) {
                print(".")
            }
            print(qualifier.name)
            if (qualifier.typeArguments.isNotEmpty()) {
                print("<")
                qualifier.typeArguments.renderSeparated()
                print(">")
            }
        }
        visitTypeWithNullability(userType)
    }

    override fun visitTypeProjection(typeProjection: FirTypeProjection) {
        visitElement(typeProjection)
    }

    override fun visitTypeProjectionWithVariance(typeProjectionWithVariance: FirTypeProjectionWithVariance) {
        typeProjectionWithVariance.variance.renderVariance()
        typeProjectionWithVariance.type.accept(this)
    }

    override fun visitStarProjection(starProjection: FirStarProjection) {
        print("*")
    }

}