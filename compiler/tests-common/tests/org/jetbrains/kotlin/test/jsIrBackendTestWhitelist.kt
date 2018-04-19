/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import java.io.File

val JS_IR_BACKEND_TEST_WHITELIST = listOf(
    "js/js.translator/testData/box/examples/funDelegation.kt",
    "js/js.translator/testData/box/examples/incrementProperty.kt",
    "js/js.translator/testData/box/examples/inheritedMethod.kt",
    "js/js.translator/testData/box/examples/initializerBlock.kt",
    "js/js.translator/testData/box/examples/kt242.kt",
    "js/js.translator/testData/box/examples/newInstanceDefaultConstructor.kt",
    "js/js.translator/testData/box/examples/propertyDelegation.kt",
    "js/js.translator/testData/box/examples/rightHandOverride.kt",

    "js/js.translator/testData/box/expression/equals/stringsEqual.kt",

    "js/js.translator/testData/box/expression/evaluationOrder/ifAsPlusArgument.kt",
    "js/js.translator/testData/box/expression/evaluationOrder/secondaryConstructorTemporaryVars.kt",

    "js/js.translator/testData/box/expression/identifierClash/overloadedFun.kt",
    "js/js.translator/testData/box/expression/identifierClash/useVariableOfNameOfFunction.kt",

    "js/js.translator/testData/box/expression/identityEquals/identityEqualsMethod.kt",

    "js/js.translator/testData/box/expression/invoke/internalFunctionFromSuperclass.kt",
    "js/js.translator/testData/box/expression/invoke/invokeWithDispatchReceiver.kt",

    "js/js.translator/testData/box/expression/misc/classWithoutPackage.kt",
    "js/js.translator/testData/box/expression/misc/KT-740-3.kt",
    "js/js.translator/testData/box/expression/misc/localProperty.kt",
    "js/js.translator/testData/box/expression/misc/propertiesWithExplicitlyDefinedAccessorsWithoutBodies.kt",

    "js/js.translator/testData/box/expression/stringClass/kt2227_2.kt",
    "js/js.translator/testData/box/expression/stringClass/kt2227.kt",
    "js/js.translator/testData/box/expression/stringClass/objectToStringCallInTemplate.kt",
    "js/js.translator/testData/box/expression/stringClass/stringAssignment.kt",
    "js/js.translator/testData/box/expression/stringClass/stringConstant.kt",

    "js/js.translator/testData/box/expression/stringTemplates/nonStrings.kt",

    "js/js.translator/testData/box/expression/when/doWhileWithOneStmWhen.kt",
    "js/js.translator/testData/box/expression/when/empty.kt",
    "js/js.translator/testData/box/expression/when/ifWithOneStmWhen.kt",
    "js/js.translator/testData/box/expression/when/kt1665.kt",
    "js/js.translator/testData/box/expression/when/whenValue.kt",
    "js/js.translator/testData/box/expression/when/whenWithOneStmWhen.kt",
    "js/js.translator/testData/box/expression/when/whenWithOnlyElse.kt",
    "js/js.translator/testData/box/expression/when/whenWithoutExpression.kt",

    "js/js.translator/testData/box/extensionFunction/extensionFunctionCalledFromExtensionFunction.kt",

    "js/js.translator/testData/box/extensionProperty/inClass.kt",

    "js/js.translator/testData/box/inheritance/abstractVarOverride.kt",
    "js/js.translator/testData/box/inheritance/baseCall.kt",
    "js/js.translator/testData/box/inheritance/initializersOfBasicClassExecute.kt",
    "js/js.translator/testData/box/inheritance/methodOverride.kt",
    "js/js.translator/testData/box/inheritance/valOverride.kt",
    "js/js.translator/testData/box/inheritance/valuePassedToAncestorConstructor.kt",
    "js/js.translator/testData/box/inheritance/withInitializeMethod.kt",

    "js/js.translator/testData/box/initialize/rootPackageValInit.kt",
    "js/js.translator/testData/box/initialize/rootValInit.kt",

    "js/js.translator/testData/box/inline/sameNameOfDeclarationsInSameModule.kt",

    "js/js.translator/testData/box/multideclaration/multiValOrVar.kt",

    "js/js.translator/testData/box/multiFile/functionsVisibleFromOtherFile.kt",

    "js/js.translator/testData/box/multiPackage/createClassFromOtherPackage.kt",
    "js/js.translator/testData/box/multiPackage/createClassFromOtherPackageUsingImport.kt",
    "js/js.translator/testData/box/multiPackage/functionsVisibleFromOtherPackage.kt",
    "js/js.translator/testData/box/multiPackage/nestedPackageFunctionCalledFromOtherPackage.kt",
    "js/js.translator/testData/box/multiPackage/subpackagesWithClashingNames.kt",
    "js/js.translator/testData/box/multiPackage/subpackagesWithClashingNamesUsingImport.kt",

    "js/js.translator/testData/box/nameClashes/differenceInCapitalization.kt",
    "js/js.translator/testData/box/nameClashes/methodOverload.kt",

    "js/js.translator/testData/box/operatorOverloading/binaryDivOverload.kt",
    "js/js.translator/testData/box/operatorOverloading/compareTo.kt",
    "js/js.translator/testData/box/operatorOverloading/compareToByName.kt",
    "js/js.translator/testData/box/operatorOverloading/notOverload.kt",
    "js/js.translator/testData/box/operatorOverloading/plusOverload.kt",
    "js/js.translator/testData/box/operatorOverloading/postfixInc.kt",
    "js/js.translator/testData/box/operatorOverloading/prefixDecOverload.kt",
    "js/js.translator/testData/box/operatorOverloading/prefixIncReturnsCorrectValue.kt",
    "js/js.translator/testData/box/operatorOverloading/unaryOnIntPropertyAsStatement.kt",
    "js/js.translator/testData/box/operatorOverloading/usingModInCaseModAssignNotAvailable.kt",

    "js/js.translator/testData/box/package/classCreatedInDeeplyNestedPackage.kt",
    "js/js.translator/testData/box/package/deeplyNestedPackage.kt",
    "js/js.translator/testData/box/package/deeplyNestedPackageFunctionCalled.kt",
    "js/js.translator/testData/box/package/initializersOfNestedPackagesExecute.kt",
    "js/js.translator/testData/box/package/nestedPackage.kt",

    "js/js.translator/testData/box/propertyAccess/accessToInstanceProperty.kt",
    "js/js.translator/testData/box/propertyAccess/customGetter.kt",
    "js/js.translator/testData/box/propertyAccess/customSetter.kt",
    "js/js.translator/testData/box/propertyAccess/field.kt",
    "js/js.translator/testData/box/propertyAccess/initInstanceProperties.kt",
    "js/js.translator/testData/box/propertyAccess/initValInConstructor.kt",
    "js/js.translator/testData/box/propertyAccess/packageCustomAccessors.kt",
    "js/js.translator/testData/box/propertyAccess/packagePropertyInitializer.kt",
    "js/js.translator/testData/box/propertyAccess/packagePropertySet.kt",
    "js/js.translator/testData/box/propertyAccess/twoClassesWithProperties.kt",

    "js/js.translator/testData/box/propertyOverride/overrideExtensionProperty.kt",

    "js/js.translator/testData/box/safeCall/safeCall.kt",

    "js/js.translator/testData/box/simple/assign.kt",
    "js/js.translator/testData/box/simple/breakDoWhile.kt",
    "js/js.translator/testData/box/simple/breakWhile.kt",
    "js/js.translator/testData/box/simple/classInstantiation.kt",
    "js/js.translator/testData/box/simple/comparison.kt",
    "js/js.translator/testData/box/simple/complexExpressionAsConstructorParameter.kt",
    "js/js.translator/testData/box/simple/constructorWithParameter.kt",
    "js/js.translator/testData/box/simple/constructorWithPropertiesAsParameters.kt",
    "js/js.translator/testData/box/simple/continueDoWhile.kt",
    "js/js.translator/testData/box/simple/continueWhile.kt",
    "js/js.translator/testData/box/simple/doWhile.kt",
    "js/js.translator/testData/box/simple/doWhile2.kt",
    "js/js.translator/testData/box/simple/elseif.kt",
    "js/js.translator/testData/box/simple/if.kt",
    "js/js.translator/testData/box/simple/ifElseAsExpression.kt",
    "js/js.translator/testData/box/simple/methodDeclarationAndCall.kt",
    "js/js.translator/testData/box/simple/minusAssignOnProperty.kt",
    "js/js.translator/testData/box/simple/notBoolean.kt",
    "js/js.translator/testData/box/simple/plusAssign.kt",
    "js/js.translator/testData/box/simple/positiveAndNegativeNumbers.kt",
    "js/js.translator/testData/box/simple/prefixIntOperations.kt",
    "js/js.translator/testData/box/simple/primCtorDelegation1.kt",
    "js/js.translator/testData/box/simple/propertiesAsParametersInitialized.kt",
    "js/js.translator/testData/box/simple/propertyAccess.kt",
    "js/js.translator/testData/box/simple/secCtorDelegation1.kt",
    "js/js.translator/testData/box/simple/secCtorDelegation2.kt",
    "js/js.translator/testData/box/simple/secCtorDelegation3.kt",
    "js/js.translator/testData/box/simple/secCtorDelegation4.kt",
    "js/js.translator/testData/box/simple/simpleInitializer.kt",
    "js/js.translator/testData/box/simple/while.kt",
    "js/js.translator/testData/box/simple/while2.kt",

    "js/js.translator/testData/box/trait/funDelegation.kt"
).map { File(it) }
