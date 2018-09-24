/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

fun classOrObjectVisitor(block: (KtClassOrObject) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitClassOrObject(classOrObject: KtClassOrObject) {
            block(classOrObject)
        }
    }

fun classOrObjectRecursiveVisitor(block: (KtClassOrObject) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitClassOrObject(classOrObject: KtClassOrObject) {
            super.visitClassOrObject(classOrObject)
            block(classOrObject)
        }
    }

fun classVisitor(block: (KtClass) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitClass(klass: KtClass) {
            block(klass)
        }
    }

fun classRecursiveVisitor(block: (KtClass) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitClass(klass: KtClass) {
            super.visitClass(klass)
            block(klass)
        }
    }

fun expressionVisitor(block: (KtExpression) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitExpression(expression: KtExpression) {
            block(expression)
        }
    }

fun expressionRecursiveVisitor(block: (KtExpression) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitExpression(expression: KtExpression) {
            super.visitExpression(expression)
            block(expression)
        }
    }

fun parameterVisitor(block: (KtParameter) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitParameter(parameter: KtParameter) {
            block(parameter)
        }
    }

fun parameterRecursiveVisitor(block: (KtParameter) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitParameter(parameter: KtParameter) {
            super.visitParameter(parameter)
            block(parameter)
        }
    }

fun propertyVisitor(block: (KtProperty) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitProperty(property: KtProperty) {
            block(property)
        }
    }

fun propertyRecursiveVisitor(block: (KtProperty) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitProperty(property: KtProperty) {
            super.visitProperty(property)
            block(property)
        }
    }

fun ifExpressionVisitor(block: (KtIfExpression) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitIfExpression(ifExpression: KtIfExpression) {
            block(ifExpression)
        }
    }

fun ifExpressionRecursiveVisitor(block: (KtIfExpression) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitIfExpression(ifExpression: KtIfExpression) {
            super.visitIfExpression(ifExpression)
            block(ifExpression)
        }
    }

fun callExpressionVisitor(block: (KtCallExpression) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitCallExpression(callExpression: KtCallExpression) {
            block(callExpression)
        }
    }

fun callExpressionRecursiveVisitor(block: (KtCallExpression) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitCallExpression(callExpression: KtCallExpression) {
            super.visitCallExpression(callExpression)
            block(callExpression)
        }
    }

fun primaryConstructorVisitor(block: (KtPrimaryConstructor) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitPrimaryConstructor(primaryConstructor: KtPrimaryConstructor) {
            block(primaryConstructor)
        }
    }

fun primaryConstructorRecursiveVisitor(block: (KtPrimaryConstructor) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitPrimaryConstructor(primaryConstructor: KtPrimaryConstructor) {
            super.visitPrimaryConstructor(primaryConstructor)
            block(primaryConstructor)
        }
    }

fun destructuringDeclarationVisitor(block: (KtDestructuringDeclaration) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration) {
            block(destructuringDeclaration)
        }
    }

fun destructuringDeclarationRecursiveVisitor(block: (KtDestructuringDeclaration) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration) {
            super.visitDestructuringDeclaration(destructuringDeclaration)
            block(destructuringDeclaration)
        }
    }

fun dotQualifiedExpressionVisitor(block: (KtDotQualifiedExpression) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitDotQualifiedExpression(dotQualifiedExpression: KtDotQualifiedExpression) {
            block(dotQualifiedExpression)
        }
    }

fun dotQualifiedExpressionRecursiveVisitor(block: (KtDotQualifiedExpression) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitDotQualifiedExpression(dotQualifiedExpression: KtDotQualifiedExpression) {
            super.visitDotQualifiedExpression(dotQualifiedExpression)
            block(dotQualifiedExpression)
        }
    }

fun prefixExpressionVisitor(block: (KtPrefixExpression) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitPrefixExpression(prefixExpression: KtPrefixExpression) {
            block(prefixExpression)
        }
    }

fun prefixExpressionRecursiveVisitor(block: (KtPrefixExpression) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitPrefixExpression(prefixExpression: KtPrefixExpression) {
            super.visitPrefixExpression(prefixExpression)
            block(prefixExpression)
        }
    }

fun typeReferenceRecursiveVisitor(block: (KtTypeReference) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitTypeReference(typeReference: KtTypeReference) {
            super.visitTypeReference(typeReference)
            block(typeReference)
        }
    }

fun namedFunctionVisitor(block: (KtNamedFunction) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitNamedFunction(namedFunction: KtNamedFunction) {
            block(namedFunction)
        }
    }

fun namedFunctionRecursiveVisitor(block: (KtNamedFunction) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitNamedFunction(namedFunction: KtNamedFunction) {
            super.visitNamedFunction(namedFunction)
            block(namedFunction)
        }
    }

fun annotationEntryVisitor(block: (KtAnnotationEntry) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
            block(annotationEntry)
        }
    }

fun annotationEntryRecursiveVisitor(block: (KtAnnotationEntry) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
            super.visitAnnotationEntry(annotationEntry)
            block(annotationEntry)
        }
    }

fun lambdaExpressionVisitor(block: (KtLambdaExpression) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
            block(lambdaExpression)
        }
    }

fun lambdaExpressionRecursiveVisitor(block: (KtLambdaExpression) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
            super.visitLambdaExpression(lambdaExpression)
            block(lambdaExpression)
        }
    }

fun enumEntryVisitor(block: (KtEnumEntry) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitEnumEntry(enumEntry: KtEnumEntry) {
            block(enumEntry)
        }
    }

fun enumEntryRecursiveVisitor(block: (KtEnumEntry) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitEnumEntry(enumEntry: KtEnumEntry) {
            super.visitEnumEntry(enumEntry)
            block(enumEntry)
        }
    }

fun packageDirectiveVisitor(block: (KtPackageDirective) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitPackageDirective(packageDirective: KtPackageDirective) {
            block(packageDirective)
        }
    }

fun packageDirectiveRecursiveVisitor(block: (KtPackageDirective) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitPackageDirective(packageDirective: KtPackageDirective) {
            super.visitPackageDirective(packageDirective)
            block(packageDirective)
        }
    }

fun binaryExpressionVisitor(block: (KtBinaryExpression) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitBinaryExpression(binaryExpression: KtBinaryExpression) {
            block(binaryExpression)
        }
    }

fun binaryWithTypeRHSExpressionVisitor(block: (KtBinaryExpressionWithTypeRHS) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS) {
            block(expression)
        }
    }

fun binaryExpressionRecursiveVisitor(block: (KtBinaryExpression) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitBinaryExpression(binaryExpression: KtBinaryExpression) {
            super.visitBinaryExpression(binaryExpression)
            block(binaryExpression)
        }
    }

fun declarationVisitor(block: (KtDeclaration) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitDeclaration(declaration: KtDeclaration) {
            block(declaration)
        }
    }

fun declarationRecursiveVisitor(block: (KtDeclaration) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitDeclaration(declaration: KtDeclaration) {
            super.visitDeclaration(declaration)
            block(declaration)
        }
    }

fun simpleNameExpressionVisitor(block: (KtSimpleNameExpression) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitSimpleNameExpression(simpleNameExpression: KtSimpleNameExpression) {
            block(simpleNameExpression)
        }
    }

fun simpleNameExpressionRecursiveVisitor(block: (KtSimpleNameExpression) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitSimpleNameExpression(simpleNameExpression: KtSimpleNameExpression) {
            super.visitSimpleNameExpression(simpleNameExpression)
            block(simpleNameExpression)
        }
    }

fun propertyAccessorVisitor(block: (KtPropertyAccessor) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitPropertyAccessor(propertyAccessor: KtPropertyAccessor) {
            block(propertyAccessor)
        }
    }

fun propertyAccessorRecursiveVisitor(block: (KtPropertyAccessor) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitPropertyAccessor(propertyAccessor: KtPropertyAccessor) {
            super.visitPropertyAccessor(propertyAccessor)
            block(propertyAccessor)
        }
    }

fun referenceExpressionVisitor(block: (KtReferenceExpression) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitReferenceExpression(referenceExpression: KtReferenceExpression) {
            block(referenceExpression)
        }
    }

fun referenceExpressionRecursiveVisitor(block: (KtReferenceExpression) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitReferenceExpression(referenceExpression: KtReferenceExpression) {
            super.visitReferenceExpression(referenceExpression)
            block(referenceExpression)
        }
    }

fun valueArgumentVisitor(block: (KtValueArgument) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitArgument(valueArgument: KtValueArgument) {
            block(valueArgument)
        }
    }

fun valueArgumentRecursiveVisitor(block: (KtValueArgument) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitArgument(valueArgument: KtValueArgument) {
            super.visitArgument(valueArgument)
            block(valueArgument)
        }
    }

fun whenExpressionVisitor(block: (KtWhenExpression) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitWhenExpression(whenExpression: KtWhenExpression) {
            block(whenExpression)
        }
    }

fun whenExpressionRecursiveVisitor(block: (KtWhenExpression) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitWhenExpression(whenExpression: KtWhenExpression) {
            super.visitWhenExpression(whenExpression)
            block(whenExpression)
        }
    }

fun modifierListVisitor(block: (KtModifierList) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitModifierList(modifierList: KtModifierList) {
            block(modifierList)
        }
    }

fun modifierListRecursiveVisitor(block: (KtModifierList) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitModifierList(modifierList: KtModifierList) {
            super.visitModifierList(modifierList)
            block(modifierList)
        }
    }

fun namedDeclarationVisitor(block: (KtNamedDeclaration) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitNamedDeclaration(namedDeclaration: KtNamedDeclaration) {
            block(namedDeclaration)
        }
    }

fun namedDeclarationRecursiveVisitor(block: (KtNamedDeclaration) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitNamedDeclaration(namedDeclaration: KtNamedDeclaration) {
            super.visitNamedDeclaration(namedDeclaration)
            block(namedDeclaration)
        }
    }

fun qualifiedExpressionVisitor(block: (KtQualifiedExpression) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitQualifiedExpression(qualifiedExpression: KtQualifiedExpression) {
            block(qualifiedExpression)
        }
    }

fun qualifiedExpressionRecursiveVisitor(block: (KtQualifiedExpression) -> Unit) =
    object : KtTreeVisitorVoid() {
        override fun visitQualifiedExpression(qualifiedExpression: KtQualifiedExpression) {
            super.visitQualifiedExpression(qualifiedExpression)
            block(qualifiedExpression)
        }
    }

fun returnExpressionVisitor(block: (KtReturnExpression) -> Unit) =
    object : KtVisitorVoid() {
        override fun visitReturnExpression(returnExpression: KtReturnExpression) {
            super.visitReturnExpression(returnExpression)
            block(returnExpression)
        }
    }
