import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Function 'getClassToEnumFoo' can not be called: Function uses unlinked class symbol '/ClassToEnum.Foo'")) { getClassToEnumFoo() }
    expectFailure(linkage("Constructor 'Foo.<init>' can not be called: No constructor found for symbol '/ClassToEnum.Foo.<init>'")) { getClassToEnumFooAsAny() }
    expectFailure(linkage("Function 'getClassToEnumBar' can not be called: Function uses unlinked class symbol '/ClassToEnum.Bar'")) { getClassToEnumBar() }
    expectFailure(linkage("Can not get instance of singleton 'Bar': No class found for symbol '/ClassToEnum.Bar'")) { getClassToEnumBarAsAny() }
    expectFailure(linkage("Function 'getClassToEnumBaz' can not be called: Function uses unlinked class symbol '/ClassToEnum.Baz'")) { getClassToEnumBaz() }
    expectFailure(linkage("Constructor 'ClassToEnum.<init>' can not be called: Private constructor declared in module <lib1> can not be accessed in module <lib2>")) { getClassToEnumBazAsAny() }

    expectFailure(linkage("Function 'getObjectToEnumFoo' can not be called: Function uses unlinked class symbol '/ObjectToEnum.Foo'")) { getObjectToEnumFoo() }
    expectFailure(linkage("Constructor 'Foo.<init>' can not be called: No constructor found for symbol '/ObjectToEnum.Foo.<init>'")) { getObjectToEnumFooAsAny() }
    expectFailure(linkage("Function 'getObjectToEnumBar' can not be called: Function uses unlinked class symbol '/ObjectToEnum.Bar'")) { getObjectToEnumBar() }
    expectFailure(linkage("Can not get instance of singleton 'Bar': No class found for symbol '/ObjectToEnum.Bar'")) { getObjectToEnumBarAsAny() }

    expectFailure(linkage("Can not get instance of singleton 'EnumToClass.Foo': No enum entry found for symbol '/EnumToClass.Foo'")) { getEnumToClassFoo() }
    expectFailure(linkage("Can not get instance of singleton 'EnumToClass.Foo': No enum entry found for symbol '/EnumToClass.Foo'")) { getEnumToClassFooAsAny() }
    expectFailure(linkage("Can not get instance of singleton 'EnumToClass.Bar': No enum entry found for symbol '/EnumToClass.Bar'")) { getEnumToClassBar() }
    expectFailure(linkage("Can not get instance of singleton 'EnumToClass.Bar': No enum entry found for symbol '/EnumToClass.Bar'")) { getEnumToClassBarAsAny() }
    expectFailure(linkage("Can not get instance of singleton 'EnumToClass.Baz': No enum entry found for symbol '/EnumToClass.Baz'")) { getEnumToClassBaz() }
    expectFailure(linkage("Can not get instance of singleton 'EnumToClass.Baz': No enum entry found for symbol '/EnumToClass.Baz'")) { getEnumToClassBazAsAny() }

    expectFailure(linkage("Can not get instance of singleton 'EnumToObject.Foo': No enum entry found for symbol '/EnumToObject.Foo'")) { getEnumToObjectFoo() }
    expectFailure(linkage("Can not get instance of singleton 'EnumToObject.Foo': No enum entry found for symbol '/EnumToObject.Foo'")) { getEnumToObjectFooAsAny() }
    expectFailure(linkage("Can not get instance of singleton 'EnumToObject.Bar': No enum entry found for symbol '/EnumToObject.Bar'")) { getEnumToObjectBar() }
    expectFailure(linkage("Can not get instance of singleton 'EnumToObject.Bar': No enum entry found for symbol '/EnumToObject.Bar'")) { getEnumToObjectBarAsAny() }

    expectFailure(linkage("Constructor 'ClassToObject.<init>' can not be called: Private constructor declared in module <lib1> can not be accessed in module <lib2>")) { getClassToObject() }
    expectFailure(linkage("Constructor 'ClassToObject.<init>' can not be called: Private constructor declared in module <lib1> can not be accessed in module <lib2>")) { getClassToObjectAsAny() }

    expectFailure(linkage("Can not get instance of singleton 'ObjectToClass': 'ObjectToClass' is class while object is expected")) { getObjectToClass() }
    expectFailure(linkage("Can not get instance of singleton 'ObjectToClass': 'ObjectToClass' is class while object is expected")) { getObjectToClassAsAny() }

    expectFailure(linkage("Constructor 'ClassToInterface.<init>' can not be called: No constructor found for symbol '/ClassToInterface.<init>'")) { getClassToInterface() }
    expectFailure(linkage("Constructor 'ClassToInterface.<init>' can not be called: No constructor found for symbol '/ClassToInterface.<init>'")) { getClassToInterfaceAsAny() }

    expectSuccess("NestedObjectToCompanion1.Companion") { getNestedObjectToCompanion1().toString() }
    expectSuccess("NestedObjectToCompanion1.Companion") { getNestedObjectToCompanion1AsAny().toString() }

    expectSuccess("NestedObjectToCompanion2.Foo") { getNestedObjectToCompanion2().toString() }
    expectSuccess("NestedObjectToCompanion2.Foo") { getNestedObjectToCompanion2AsAny().toString() }

    expectSuccess("CompanionToNestedObject1.Companion") { getCompanionToNestedObject1().toString() }
    expectSuccess("CompanionToNestedObject1.Companion") { getCompanionToNestedObject1AsAny().toString() }
    expectSuccess("CompanionToNestedObject1.Companion") { getCompanionToNestedObject1Name() }
    expectSuccess("CompanionToNestedObject1.Companion") { getCompanionToNestedObject1NameShort() }

    expectSuccess("CompanionToNestedObject2.Foo") { getCompanionToNestedObject2().toString() }
    expectSuccess("CompanionToNestedObject2.Foo") { getCompanionToNestedObject2AsAny().toString() }
    expectSuccess("CompanionToNestedObject2.Foo") { getCompanionToNestedObject2Name() }
    expectSuccess("CompanionToNestedObject2.Foo") { getCompanionToNestedObject2NameShort() }

    expectSuccess("Foo") { getCompanionAndNestedObjectsSwap() }

    expectFailure(linkage("Constructor 'NestedToInner.<init>' can not be called: The call site has 1 less value argument(s) than the constructor requires. Those arguments are missing: <this>")) { getNestedToInnerName() }
    expectFailure(linkage("Can not get instance of singleton 'Object': 'Object' is inner class while object is expected")) { getNestedToInnerObjectName() }
    expectFailure(linkage("Can not get instance of singleton 'Companion': 'Companion' is inner class while object is expected")) { getNestedToInnerCompanionName() }
    expectFailure(linkage("Constructor 'Nested.<init>' can not be called: The call site has 1 less value argument(s) than the constructor requires. Those arguments are missing: <this>")) { getNestedToInnerNestedName() }
    expectFailure(linkage("Constructor 'NestedToInner.<init>' can not be called: The call site has 1 less value argument(s) than the constructor requires. Those arguments are missing: <this>")) { getNestedToInnerInnerName() }

    expectFailure(linkage("Constructor 'InnerToNested.<init>' can not be called: The call site provides 1 more value argument(s) than the constructor expects")) { getInnerToNestedName() }
    expectFailure(linkage("Constructor 'InnerToNested.<init>' can not be called: The call site provides 1 more value argument(s) than the constructor expects")) { getInnerToNestedObjectName() }
    expectFailure(linkage("Constructor 'InnerToNested.<init>' can not be called: The call site provides 1 more value argument(s) than the constructor expects")) { getInnerToNestedCompanionName() }
    expectFailure(linkage("Constructor 'InnerToNested.<init>' can not be called: The call site provides 1 more value argument(s) than the constructor expects")) { getInnerToNestedNestedName() }
    expectFailure(linkage("Constructor 'InnerToNested.<init>' can not be called: The call site provides 1 more value argument(s) than the constructor expects")) { getInnerToNestedInnerName() }

    expectFailure(linkage("Constructor 'AnnotationClassWithChangedParameterType.<init>' can not be called: No constructor found for symbol '/AnnotationClassWithChangedParameterType.<init>'")) { getAnnotationClassWithChangedParameterType() }
    expectFailure(linkage("Constructor 'AnnotationClassWithChangedParameterType.<init>' can not be called: No constructor found for symbol '/AnnotationClassWithChangedParameterType.<init>'")) { getAnnotationClassWithChangedParameterTypeAsAny() }
    expectSuccess(105) { getAnnotationClassThatBecomesRegularClass().x }
    expectSuccess("AnnotationClassThatBecomesRegularClass[x=107]") { getAnnotationClassThatBecomesRegularClassAsAny().toString() }
    expectFailure(linkage("Function 'getAnnotationClassWithParameterThatBecomesRegularClass' can not be called: Function uses annotation class 'AnnotationClassWithParameterThatBecomesRegularClass' that has non-annotation class 'AnnotationClassThatBecomesRegularClass' as a parameter")) { getAnnotationClassWithParameterThatBecomesRegularClass().x.x }
    expectFailure(linkage("Constructor 'AnnotationClassWithParameterThatBecomesRegularClass.<init>' can not be called: Annotation class 'AnnotationClassWithParameterThatBecomesRegularClass' uses non-annotation class 'AnnotationClassThatBecomesRegularClass' as a parameter")) { getAnnotationClassWithParameterThatBecomesRegularClassAsAny().toString() }
    expectFailure(linkage("Function 'getAnnotationClassWithParameterOfParameterThatBecomesRegularClass' can not be called: Function uses annotation class 'AnnotationClassWithParameterThatBecomesRegularClass' (via annotation class 'AnnotationClassWithParameterOfParameterThatBecomesRegularClass') that has non-annotation class 'AnnotationClassThatBecomesRegularClass' as a parameter")) { getAnnotationClassWithParameterOfParameterThatBecomesRegularClass().x.x.x }
    expectFailure(linkage("Constructor 'AnnotationClassWithParameterThatBecomesRegularClass.<init>' can not be called: Annotation class 'AnnotationClassWithParameterThatBecomesRegularClass' uses non-annotation class 'AnnotationClassThatBecomesRegularClass' as a parameter")) { getAnnotationClassWithParameterOfParameterThatBecomesRegularClassAsAny().toString() }
    expectFailure(linkage("Function 'getAnnotationClassThatDisappears' can not be called: Function uses unlinked class symbol '/AnnotationClassThatDisappears'")) { getAnnotationClassThatDisappears() }
    expectFailure(linkage("Constructor 'AnnotationClassThatDisappears.<init>' can not be called: No constructor found for symbol '/AnnotationClassThatDisappears.<init>'")) { getAnnotationClassThatDisappearsAsAny() }
    expectFailure(linkage("Function 'getAnnotationClassWithParameterThatDisappears' can not be called: Function uses unlinked class symbol '/AnnotationClassThatDisappears' (via annotation class 'AnnotationClassWithParameterThatDisappears')")) { getAnnotationClassWithParameterThatDisappears() }
    expectFailure(linkage("Function 'getAnnotationClassWithParameterOfParameterThatDisappears' can not be called: Function uses unlinked class symbol '/AnnotationClassThatDisappears' (via annotation class 'AnnotationClassWithParameterOfParameterThatDisappears')")) { getAnnotationClassWithParameterOfParameterThatDisappears() }
    expectFailure(linkage("Constructor 'AnnotationClassThatDisappears.<init>' can not be called: No constructor found for symbol '/AnnotationClassThatDisappears.<init>'")) { getAnnotationClassWithParameterThatDisappearsAsAny() }
    if (!testMode.isWasm) { //toString for annotation classes are not supported KT-66385
        expectSuccess("@AnnotationClassWithRenamedParameters(xi=129, xs=Banana)") { getAnnotationClassWithRenamedParameters().toString() }
        expectSuccess("@AnnotationClassWithRenamedParameters(xi=131, xs=Orange)") { getAnnotationClassWithRenamedParametersAsAny().toString() }
    }
    expectFailure(linkage("Constructor 'AnnotationClassWithReorderedParameters.<init>' can not be called: No constructor found for symbol '/AnnotationClassWithReorderedParameters.<init>'")) { getAnnotationClassWithReorderedParameters() }
    expectFailure(linkage("Constructor 'AnnotationClassWithReorderedParameters.<init>' can not be called: No constructor found for symbol '/AnnotationClassWithReorderedParameters.<init>'")) { getAnnotationClassWithReorderedParametersAsAny() }
    expectFailure(linkage("Constructor 'AnnotationClassWithNewParameter.<init>' can not be called: No constructor found for symbol '/AnnotationClassWithNewParameter.<init>'")) { getAnnotationClassWithNewParameter() }
    expectFailure(linkage("Constructor 'AnnotationClassWithNewParameter.<init>' can not be called: No constructor found for symbol '/AnnotationClassWithNewParameter.<init>'")) { getAnnotationClassWithNewParameterAsAny() }
    expectFailure(linkage("Function 'getAnnotationClassWithClassReferenceParameterThatDisappears1' can not be called: Function uses unlinked class symbol '/RemovedClass' (via annotation class 'AnnotationClassWithClassReferenceParameterThatDisappears1')")) { getAnnotationClassWithClassReferenceParameterThatDisappears1() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithClassReferenceParameterThatDisappears1AsAny() }
    expectFailure(linkage("Function 'getAnnotationClassWithDefaultClassReferenceParameterThatDisappears1' can not be called: Function uses unlinked class symbol '/RemovedClass' (via annotation class 'AnnotationClassWithClassReferenceParameterThatDisappears1')")) { getAnnotationClassWithDefaultClassReferenceParameterThatDisappears1() }
    expectFailure(linkage("Constructor 'AnnotationClassWithClassReferenceParameterThatDisappears1.<init>' can not be called: Constructor uses unlinked class symbol '/RemovedClass'")) { getAnnotationClassWithDefaultClassReferenceParameterThatDisappears1AsAny() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithClassReferenceParameterThatDisappears2() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithClassReferenceParameterThatDisappears2AsAny() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithDefaultClassReferenceParameterThatDisappears2() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithDefaultClassReferenceParameterThatDisappears2AsAny() }
    expectFailure(linkage("Function 'getAnnotationClassWithClassReferenceParameterOfParameterThatDisappears1' can not be called: Function uses unlinked class symbol '/RemovedClass' (via annotation class 'AnnotationClassWithClassReferenceParameterOfParameterThatDisappears1')")) { getAnnotationClassWithClassReferenceParameterOfParameterThatDisappears1() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithClassReferenceParameterOfParameterThatDisappears1AsAny() }
    expectFailure(linkage("Function 'getAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears1' can not be called: Function uses unlinked class symbol '/RemovedClass' (via annotation class 'AnnotationClassWithClassReferenceParameterOfParameterThatDisappears1')")) { getAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears1() }
    expectFailure(linkage("Constructor 'AnnotationClassWithClassReferenceParameterOfParameterThatDisappears1.<init>' can not be called: Constructor uses unlinked class symbol '/RemovedClass' (via annotation class 'AnnotationClassWithClassReferenceParameterThatDisappears1')")) { getAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears1AsAny() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithClassReferenceParameterOfParameterThatDisappears2() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithClassReferenceParameterOfParameterThatDisappears2AsAny() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears2() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears2AsAny() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassWithDisappearingEntry.REMOVED': No enum entry found for symbol '/EnumClassWithDisappearingEntry.REMOVED'")) { getAnnotationClassWithRemovedEnumEntryParameter() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassWithDisappearingEntry.REMOVED': No enum entry found for symbol '/EnumClassWithDisappearingEntry.REMOVED'")) { getAnnotationClassWithRemovedEnumEntryParameterAsAny() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassWithDisappearingEntry.REMOVED': No enum entry found for symbol '/EnumClassWithDisappearingEntry.REMOVED'")) { getAnnotationClassWithDefaultRemovedEnumEntryParameter() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassWithDisappearingEntry.REMOVED': No enum entry found for symbol '/EnumClassWithDisappearingEntry.REMOVED'")) { getAnnotationClassWithDefaultRemovedEnumEntryParameterAsAny() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassWithDisappearingEntry.REMOVED': No enum entry found for symbol '/EnumClassWithDisappearingEntry.REMOVED'")) { getAnnotationClassWithRemovedEnumEntryParameterOfParameter() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassWithDisappearingEntry.REMOVED': No enum entry found for symbol '/EnumClassWithDisappearingEntry.REMOVED'")) { getAnnotationClassWithRemovedEnumEntryParameterOfParameterAsAny() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassWithDisappearingEntry.REMOVED': No enum entry found for symbol '/EnumClassWithDisappearingEntry.REMOVED'")) { getAnnotationClassWithDefaultRemovedEnumEntryParameterOfParameter() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassWithDisappearingEntry.REMOVED': No enum entry found for symbol '/EnumClassWithDisappearingEntry.REMOVED'")) { getAnnotationClassWithDefaultRemovedEnumEntryParameterOfParameterAsAny() }
    expectFailure(linkage("Constructor 'AnnotationClassThatBecomesPrivate.<init>' can not be called: Private constructor declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterThatBecomesPrivate1() }
    expectFailure(linkage("Constructor 'AnnotationClassThatBecomesPrivate.<init>' can not be called: Private constructor declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterThatBecomesPrivate1AsAny() }
    expectFailure(linkage("Reference to class 'ClassThatBecomesPrivate' can not be evaluated: Private class declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterThatBecomesPrivate2() }
    expectFailure(linkage("Reference to class 'ClassThatBecomesPrivate' can not be evaluated: Private class declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterThatBecomesPrivate2AsAny() }
    expectFailure(linkage("Reference to class 'ClassThatBecomesPrivate' can not be evaluated: Private class declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterOfParameterThatBecomesPrivate2() }
    expectFailure(linkage("Reference to class 'ClassThatBecomesPrivate' can not be evaluated: Private class declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterOfParameterThatBecomesPrivate2AsAny() }
    expectFailure(linkage("Reference to class 'ClassThatBecomesPrivate' can not be evaluated: Private class declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterThatBecomesPrivate3() }
    expectFailure(linkage("Reference to class 'ClassThatBecomesPrivate' can not be evaluated: Private class declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterThatBecomesPrivate3AsAny() }
    expectFailure(linkage("Reference to class 'ClassThatBecomesPrivate' can not be evaluated: Private class declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterOfParameterThatBecomesPrivate3() }
    expectFailure(linkage("Reference to class 'ClassThatBecomesPrivate' can not be evaluated: Private class declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterOfParameterThatBecomesPrivate3AsAny() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassThatBecomesPrivate.ENTRY': Private enum entry declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterThatBecomesPrivate4().toString() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassThatBecomesPrivate.ENTRY': Private enum entry declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterThatBecomesPrivate4AsAny().toString() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassThatBecomesPrivate.ENTRY': Private enum entry declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterOfParameterThatBecomesPrivate4().toString() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassThatBecomesPrivate.ENTRY': Private enum entry declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterOfParameterThatBecomesPrivate4AsAny().toString() }
    expectSuccess { getAnnotationClassWithParameterWithPrivateDefaultValue(); "OK" }
    expectSuccess { getAnnotationClassWithParameterWithPrivateDefaultValueAsAny(); "OK" }
    expectSuccess { getAnnotationClassWithParameterOfParameterWithPrivateDefaultValue(); "OK" }
    expectSuccess { getAnnotationClassWithParameterOfParameterWithPrivateDefaultValueAsAny(); "OK" }

    // Handle unlinked constructor call in annotation & non-annotation class appearing in annotation:
    expectSuccess("HolderOfAnnotationClassWithChangedParameterType") { getHolderOfAnnotationClassWithChangedParameterType().toString() }
    expectSuccess("HolderOfAnnotationClassThatBecomesRegularClass") { getHolderOfAnnotationClassThatBecomesRegularClass().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterThatBecomesRegularClass") { getHolderOfAnnotationClassWithParameterThatBecomesRegularClass().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterOfParameterThatBecomesRegularClass") { getHolderOfAnnotationClassWithParameterOfParameterThatBecomesRegularClass().toString() }
    expectSuccess("HolderOfAnnotationClassThatDisappears") { getHolderOfAnnotationClassThatDisappears().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterThatDisappears") { getHolderOfAnnotationClassWithParameterThatDisappears().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterOfParameterThatDisappears") { getHolderOfAnnotationClassWithParameterOfParameterThatDisappears().toString() }
    expectSuccess("HolderOfAnnotationClassWithRenamedParameters") { getHolderOfAnnotationClassWithRenamedParameters().toString() }
    expectSuccess("HolderOfAnnotationClassWithReorderedParameters") { getHolderOfAnnotationClassWithReorderedParameters().toString() }
    expectSuccess("HolderOfAnnotationClassWithNewParameter") { getHolderOfAnnotationClassWithNewParameter().toString() }
    expectSuccess("HolderOfAnnotationClassWithClassReferenceParameterThatDisappears1") { getHolderOfAnnotationClassWithClassReferenceParameterThatDisappears1().toString() }
    expectSuccess("HolderOfAnnotationClassWithDefaultClassReferenceParameterThatDisappears1") { getHolderOfAnnotationClassWithDefaultClassReferenceParameterThatDisappears1().toString() }
    expectSuccess("HolderOfAnnotationClassWithClassReferenceParameterThatDisappears2") { getHolderOfAnnotationClassWithClassReferenceParameterThatDisappears2().toString() }
    expectSuccess("HolderOfAnnotationClassWithDefaultClassReferenceParameterThatDisappears2") { getHolderOfAnnotationClassWithDefaultClassReferenceParameterThatDisappears2().toString() }
    expectSuccess("HolderOfAnnotationClassWithClassReferenceParameterOfParameterThatDisappears1") { getHolderOfAnnotationClassWithClassReferenceParameterOfParameterThatDisappears1().toString() }
    expectSuccess("HolderOfAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears1") { getHolderOfAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears1().toString() }
    expectSuccess("HolderOfAnnotationClassWithClassReferenceParameterOfParameterThatDisappears2") { getHolderOfAnnotationClassWithClassReferenceParameterOfParameterThatDisappears2().toString() }
    expectSuccess("HolderOfAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears2") { getHolderOfAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears2().toString() }
    expectSuccess("HolderOfAnnotationClassWithRemovedEnumEntryParameter") { getHolderOfAnnotationClassWithRemovedEnumEntryParameter().toString() }
    expectSuccess("HolderOfAnnotationClassWithDefaultRemovedEnumEntryParameter") { getHolderOfAnnotationClassWithDefaultRemovedEnumEntryParameter().toString() }
    expectSuccess("HolderOfAnnotationClassWithRemovedEnumEntryParameterOfParameter") { getHolderOfAnnotationClassWithRemovedEnumEntryParameterOfParameter().toString() }
    expectSuccess("HolderOfAnnotationClassWithDefaultRemovedEnumEntryParameterOfParameter") { getHolderOfAnnotationClassWithDefaultRemovedEnumEntryParameterOfParameter().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterThatBecomesPrivate1") { getHolderOfAnnotationClassWithParameterThatBecomesPrivate1().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterThatBecomesPrivate2") { getHolderOfAnnotationClassWithParameterThatBecomesPrivate2().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate2") { getHolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate2().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterThatBecomesPrivate3") { getHolderOfAnnotationClassWithParameterThatBecomesPrivate3().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate3") { getHolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate3().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterThatBecomesPrivate4") { getHolderOfAnnotationClassWithParameterThatBecomesPrivate4().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate4") { getHolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate4().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterWithPrivateDefaultValue") { getHolderOfAnnotationClassWithParameterWithPrivateDefaultValue().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterOfParameterWithPrivateDefaultValue") { getHolderOfAnnotationClassWithParameterOfParameterWithPrivateDefaultValue().toString() }

    expectSuccess { getValueToClass(); "OK" }
    expectSuccess { getValueToClassAsAny(); "OK" }

    expectSuccess { getClassToValue(); "OK" }
    expectSuccess { getClassToValueAsAny(); "OK" }

    expectFailure(linkage("Function 'component1' can not be called: No function found for symbol '/DataToClass.component1'")) { getSumFromDataClass() }

    expectFailure(linkage("Constructor 'ClassToAbstractClass.<init>' can not be called: Can not instantiate abstract class 'ClassToAbstractClass'")) { instantiationOfAbstractClass() }

    expectSuccess { StableEnum.FOO.test }
    expectSuccess { StableEnum.BAR.test }

    expectSuccess("01234") { testStableInheritorOfClassThatUsesPrivateTopLevelClass() }
}
