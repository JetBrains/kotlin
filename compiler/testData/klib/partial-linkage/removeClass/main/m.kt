import abitestutils.abiTest

fun box() = abiTest {
    val checker = Checker()

    expectFailure(linkage("Constructor 'RemovedClass.<init>' can not be called: No constructor found for symbol '/RemovedClass.<init>'")) { createRemovedClass() }

    expectFailure(linkage("Function 'useRemovedClassAsValueParameter' can not be called: Function uses unlinked class symbol '/RemovedClass'")) { checker.createAndPassRemovedClassAsValueParameter() }
    expectFailure(linkage("Property accessor 'removedClassProperty.<set-removedClassProperty>' can not be called: Property accessor uses unlinked class symbol '/RemovedClass'")) { checker.writeToRemovedClassProperty() }
    expectSuccess("Checker.useClassAsValueParameter(Class)") { checker.createAndPassClassAsValueParameter() }

    expectFailure(linkage("Can not read value from variable 'removed': Variable uses unlinked class symbol '/RemovedClass'")) { readVariableInFunction() }
    expectFailure(linkage("Can not write value to variable 'removed': Variable uses unlinked class symbol '/RemovedClass'")) { writeVariableInFunction() }
    expectFailure(linkage("Can not read value from variable 'removed': Variable uses unlinked class symbol '/RemovedClass'")) { readVariableInLocalFunction() }
    expectFailure(linkage("Can not write value to variable 'removed': Variable uses unlinked class symbol '/RemovedClass'")) { writeVariableInLocalFunction() }
    expectFailure(linkage("Can not read value from variable 'removed': Variable uses unlinked class symbol '/RemovedClass'")) { readVariableInLocalClass() }
    expectFailure(linkage("Can not write value to variable 'removed': Variable uses unlinked class symbol '/RemovedClass'")) { writeVariableInLocalClass() }
    expectFailure(linkage("Can not read value from variable 'removed': Variable uses unlinked class symbol '/RemovedClass'")) { readVariableInAnonymousObject() }
    expectFailure(linkage("Can not write value to variable 'removed': Variable uses unlinked class symbol '/RemovedClass'")) { writeVariableInAnonymousObject() }
    expectFailure(linkage("Can not read value from variable 'removed': Variable uses unlinked class symbol '/RemovedClass'")) { readVariableInAnonymousObjectThroughLocalVar() }
    expectFailure(linkage("Can not write value to variable 'removed': Variable uses unlinked class symbol '/RemovedClass'")) { writeVariableInAnonymousObjectThroughLocalVar() }

    expectFailure(linkage("Function 'createRemovedClass' can not be called: Function uses unlinked class symbol '/RemovedClass'")) { checker.createRemovedClassAndCallFunction() }
    expectFailure(linkage("Property accessor 'getRemovedClass.<get-getRemovedClass>' can not be called: Property accessor uses unlinked class symbol '/RemovedClass'")) { checker.getRemovedClassAndReadProperty }
    expectSuccess("Class.f") { checker.createClassAndCallFunction() }
    expectSuccess("Class.p") { checker.getClassAndReadProperty1 }
    expectSuccess("Class.p") { checker.getClassAndReadProperty2 }
    expectFailure(linkage("Property accessor 'getRemovedClass.<get-getRemovedClass>' can not be called: Property accessor uses unlinked class symbol '/RemovedClass'")) { Checker.CrashesOnCreation() }

    expectFailure(linkage("Function 'local' can not be called: Function uses unlinked class symbol '/RemovedClass'")) { callLocalFunction() }
    expectFailure(linkage("Function 'local' can not be called: Function uses unlinked class symbol '/RemovedClass'")) { callLocalFunctionInLocalFunction() }
    expectFailure(linkage("Function 'local' can not be called: Function uses unlinked class symbol '/RemovedClass'")) { callLocalFunctionInFunctionOfLocalClass() }
    expectFailure(linkage("Function 'local' can not be called: Function uses unlinked class symbol '/RemovedClass'")) { callLocalFunctionInFunctionOfAnonymousObject() }
    expectFailure(linkage("Function 'local' can not be called: Function uses unlinked class symbol '/RemovedClass'")) { callLocalFunctionInFunctionOfAnonymousObjectThroughLocalVar() }

    expectFailure(linkage("Function 'createInstanceImplParameterizedByRemovedClass' can not be called: Function uses unlinked class symbol '/RemovedClass'")) { checker.createInstanceImplParameterizedByRemovedClassAndCallFunction() }
    expectSuccess("Class.f") { checker.createInterfaceImplParameterizedByClassAndCallFunction() }

    expectFailure(linkage("Constructor 'TopLevelClassChildOfRemovedAbstractClass.<init>' can not be called: Class 'TopLevelClassChildOfRemovedAbstractClass' uses unlinked class symbol '/RemovedAbstractClass'")) { TopLevelClassChildOfRemovedAbstractClass() }
    expectFailure(linkage("Can not get instance of singleton 'TopLevelObjectChildOfRemovedAbstractClass': Expression uses unlinked class symbol '/RemovedAbstractClass'")) { TopLevelObjectChildOfRemovedAbstractClass }
    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object uses unlinked class symbol '/RemovedInterface'")) { object : TopLevelInterfaceChildOfRemovedInterface {} }
    expectFailure(linkage("Constructor 'TopLevelClassChildOfRemovedInterface.<init>' can not be called: Class 'TopLevelClassChildOfRemovedInterface' uses unlinked class symbol '/RemovedInterface'")) { TopLevelClassChildOfRemovedInterface() }
    expectFailure(linkage("Can not get instance of singleton 'TopLevelObjectChildOfRemovedInterface': Expression uses unlinked class symbol '/RemovedInterface'")) { TopLevelObjectChildOfRemovedInterface }
    expectFailure(linkage("Can not get instance of singleton 'TopLevelEnumClassChildOfRemovedInterface.ENTRY': Expression uses unlinked class symbol '/RemovedInterface'")) { TopLevelEnumClassChildOfRemovedInterface.ENTRY }
    expectFailure(linkage("Constructor 'NestedClassChildOfRemovedAbstractClass.<init>' can not be called: Class 'NestedClassChildOfRemovedAbstractClass' uses unlinked class symbol '/RemovedAbstractClass'")) { TopLevel.NestedClassChildOfRemovedAbstractClass() }
    expectFailure(linkage("Can not get instance of singleton 'NestedObjectChildOfRemovedAbstractClass': Expression uses unlinked class symbol '/RemovedAbstractClass'")) { TopLevel.NestedObjectChildOfRemovedAbstractClass }
    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object uses unlinked class symbol '/RemovedInterface'")) { object : TopLevel.NestedInterfaceChildOfRemovedInterface {} }
    expectFailure(linkage("Constructor 'NestedClassChildOfRemovedInterface.<init>' can not be called: Class 'NestedClassChildOfRemovedInterface' uses unlinked class symbol '/RemovedInterface'")) { TopLevel.NestedClassChildOfRemovedInterface() }
    expectFailure(linkage("Can not get instance of singleton 'NestedObjectChildOfRemovedInterface': Expression uses unlinked class symbol '/RemovedInterface'")) { TopLevel.NestedObjectChildOfRemovedInterface }
    expectFailure(linkage("Can not get instance of singleton 'NestedEnumClassChildOfRemovedInterface.ENTRY': Expression uses unlinked class symbol '/RemovedInterface'")) { TopLevel.NestedEnumClassChildOfRemovedInterface.ENTRY }
    expectFailure(linkage("Constructor 'InnerClassChildOfRemovedAbstractClass.<init>' can not be called: Inner class 'InnerClassChildOfRemovedAbstractClass' uses unlinked class symbol '/RemovedAbstractClass'")) { TopLevel().InnerClassChildOfRemovedAbstractClass() }
    expectFailure(linkage("Constructor 'InnerClassChildOfRemovedInterface.<init>' can not be called: Inner class 'InnerClassChildOfRemovedInterface' uses unlinked class symbol '/RemovedInterface'")) { TopLevel().InnerClassChildOfRemovedInterface() }
    expectFailure(linkage("Can not get instance of singleton 'TopLevelWithCompanionChildOfRemovedAbstractClass.Companion': Expression uses unlinked class symbol '/RemovedAbstractClass'")) { TopLevelWithCompanionChildOfRemovedAbstractClass.Companion }
    expectFailure(linkage("Can not get instance of singleton 'TopLevelWithCompanionChildOfRemovedInterface.Companion': Expression uses unlinked class symbol '/RemovedInterface'")) { TopLevelWithCompanionChildOfRemovedInterface.Companion }
    expectFailure(linkage("Property accessor 'anonymousObjectChildOfRemovedAbstractClass.<get-anonymousObjectChildOfRemovedAbstractClass>' can not be called: Property accessor uses unlinked class symbol '/RemovedAbstractClass'")) { anonymousObjectChildOfRemovedAbstractClass }
    expectFailure(linkage("Property accessor 'anonymousObjectChildOfRemovedInterface.<get-anonymousObjectChildOfRemovedInterface>' can not be called: Property accessor uses unlinked class symbol '/RemovedInterface'")) { anonymousObjectChildOfRemovedInterface }
    expectFailure(linkage("Constructor 'LocalClass.<init>' can not be called: Class 'LocalClass' uses unlinked class symbol '/RemovedAbstractClass'")) { topLevelFunctionWithLocalClassChildOfRemovedAbstractClass() }
    expectFailure(linkage("Constructor 'LocalClass.<init>' can not be called: Class 'LocalClass' uses unlinked class symbol '/RemovedInterface'")) { topLevelFunctionWithLocalClassChildOfRemovedInterface() }
    expectFailure(linkage("Can not read value from variable 'anonymousObject': Variable uses unlinked class symbol '/RemovedAbstractClass' (via anonymous object)")) { topLevelFunctionWithAnonymousObjectChildOfRemovedAbstractClass() }
    expectFailure(linkage("Can not read value from variable 'anonymousObject': Variable uses unlinked class symbol '/RemovedInterface' (via anonymous object)")) { topLevelFunctionWithAnonymousObjectChildOfRemovedInterface() }

    expectFailure(linkage("Can not read value from variable 'foo': Variable uses unlinked class symbol '/RemovedOpenClass'")) { inlinedFunctionWithRemovedOpenClassVariableType() }
    expectFailure(linkage("Can not read value from variable 'foo': Variable uses unlinked class symbol '/RemovedOpenClass' (via class 'OpenClassImpl')")) { inlinedFunctionWithOpenClassImplVariableType() }
    expectFailure(linkage("Constructor 'RemovedOpenClass.<init>' can not be called: No constructor found for symbol '/RemovedOpenClass.<init>'")) { inlinedFunctionWithCreationOfRemovedOpenClass() }
    expectFailure(linkage("Constructor 'OpenClassImpl.<init>' can not be called: Class 'OpenClassImpl' uses unlinked class symbol '/RemovedOpenClass'")) { inlinedFunctionWithCreationOfOpenClassImpl() }
    expectFailure(linkage("Reference to constructor 'RemovedOpenClass.<init>' can not be evaluated: No constructor found for symbol '/RemovedOpenClass.<init>'")) { inlinedFunctionWithCreationOfRemovedOpenClassThroughReference() }
    expectFailure(linkage("Reference to constructor 'OpenClassImpl.<init>' can not be evaluated: Class 'OpenClassImpl' uses unlinked class symbol '/RemovedOpenClass'")) { inlinedFunctionWithCreationOfOpenClassImplThroughReference() }
    expectFailure(linkage("Can not read value from variable 'foo': Variable uses unlinked class symbol '/RemovedOpenClass' (via anonymous object)")) { inlinedFunctionWithRemovedOpenClassAnonymousObject() }
    expectFailure(linkage("Can not read value from variable 'foo': Variable uses unlinked class symbol '/RemovedOpenClass' (via anonymous object)")) { inlinedFunctionWithOpenClassImplAnonymousObject() }
}
