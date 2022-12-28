import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Constructor 'TopLevelClassChildOfRemovedClass.<init>' can not be called: Class 'TopLevelClassChildOfRemovedClass' uses unlinked class symbol '/RemovedClass'")) { TopLevelClassChildOfRemovedClass() }
    expectFailure(linkage("Can not get instance of singleton 'TopLevelObjectChildOfRemovedClass': Expression uses unlinked class symbol '/RemovedClass'")) { TopLevelObjectChildOfRemovedClass }
    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object uses unlinked class symbol '/RemovedInterface'")) { object : TopLevelInterfaceChildOfRemovedInterface {} }
    expectFailure(linkage("Constructor 'TopLevelClassChildOfRemovedInterface.<init>' can not be called: Class 'TopLevelClassChildOfRemovedInterface' uses unlinked class symbol '/RemovedInterface'")) { TopLevelClassChildOfRemovedInterface() }
    expectFailure(linkage("Can not get instance of singleton 'TopLevelObjectChildOfRemovedInterface': Expression uses unlinked class symbol '/RemovedInterface'")) { TopLevelObjectChildOfRemovedInterface }
    expectFailure(linkage("Can not get instance of singleton 'TopLevelEnumClassChildOfRemovedInterface.ENTRY': Expression uses unlinked class symbol '/RemovedInterface'")) { TopLevelEnumClassChildOfRemovedInterface.ENTRY }
    expectFailure(linkage("Constructor 'NestedClassChildOfRemovedClass.<init>' can not be called: Class 'NestedClassChildOfRemovedClass' uses unlinked class symbol '/RemovedClass'")) { TopLevel.NestedClassChildOfRemovedClass() }
    expectFailure(linkage("Can not get instance of singleton 'NestedObjectChildOfRemovedClass': Expression uses unlinked class symbol '/RemovedClass'")) { TopLevel.NestedObjectChildOfRemovedClass }
    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object uses unlinked class symbol '/RemovedInterface'")) { object : TopLevel.NestedInterfaceChildOfRemovedInterface {} }
    expectFailure(linkage("Constructor 'NestedClassChildOfRemovedInterface.<init>' can not be called: Class 'NestedClassChildOfRemovedInterface' uses unlinked class symbol '/RemovedInterface'")) { TopLevel.NestedClassChildOfRemovedInterface() }
    expectFailure(linkage("Can not get instance of singleton 'NestedObjectChildOfRemovedInterface': Expression uses unlinked class symbol '/RemovedInterface'")) { TopLevel.NestedObjectChildOfRemovedInterface }
    expectFailure(linkage("Can not get instance of singleton 'NestedEnumClassChildOfRemovedInterface.ENTRY': Expression uses unlinked class symbol '/RemovedInterface'")) { TopLevel.NestedEnumClassChildOfRemovedInterface.ENTRY }
    expectFailure(linkage("Constructor 'InnerClassChildOfRemovedClass.<init>' can not be called: Inner class 'InnerClassChildOfRemovedClass' uses unlinked class symbol '/RemovedClass'")) { TopLevel().InnerClassChildOfRemovedClass() }
    expectFailure(linkage("Constructor 'InnerClassChildOfRemovedInterface.<init>' can not be called: Inner class 'InnerClassChildOfRemovedInterface' uses unlinked class symbol '/RemovedInterface'")) { TopLevel().InnerClassChildOfRemovedInterface() }
    expectFailure(linkage("Can not get instance of singleton 'TopLevelWithCompanionChildOfRemovedClass.Companion': Expression uses unlinked class symbol '/RemovedClass'")) { TopLevelWithCompanionChildOfRemovedClass.Companion }
    expectFailure(linkage("Can not get instance of singleton 'TopLevelWithCompanionChildOfRemovedInterface.Companion': Expression uses unlinked class symbol '/RemovedInterface'")) { TopLevelWithCompanionChildOfRemovedInterface.Companion }
    expectFailure(linkage("Property accessor 'anonymousObjectChildOfRemovedClass.<get-anonymousObjectChildOfRemovedClass>' can not be called: Property accessor uses unlinked class symbol '/RemovedClass'")) { anonymousObjectChildOfRemovedClass }
    expectFailure(linkage("Property accessor 'anonymousObjectChildOfRemovedInterface.<get-anonymousObjectChildOfRemovedInterface>' can not be called: Property accessor uses unlinked class symbol '/RemovedInterface'")) { anonymousObjectChildOfRemovedInterface }
    expectFailure(linkage("Constructor 'LocalClass.<init>' can not be called: Class 'LocalClass' uses unlinked class symbol '/RemovedClass'")) { topLevelFunctionWithLocalClassChildOfRemovedClass() }
    expectFailure(linkage("Constructor 'LocalClass.<init>' can not be called: Class 'LocalClass' uses unlinked class symbol '/RemovedInterface'")) { topLevelFunctionWithLocalClassChildOfRemovedInterface() }
    expectFailure(linkage("Can not read value from variable 'anonymousObject': Variable uses unlinked class symbol '/RemovedClass' (through anonymous object)")) { topLevelFunctionWithAnonymousObjectChildOfRemovedClass() }
    expectFailure(linkage("Can not read value from variable 'anonymousObject': Variable uses unlinked class symbol '/RemovedInterface' (through anonymous object)")) { topLevelFunctionWithAnonymousObjectChildOfRemovedInterface() }
}
