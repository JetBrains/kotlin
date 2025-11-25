import abitestutils.abiTest

fun box() = abiTest {

    /**************************************************/
    /***** Extracted from 'classTransformations': *****/
    /**************************************************/

    expectFailure(linkage("Constructor 'Foo.<init>' can not be called: No constructor found for symbol '/ClassToEnum.Foo.<init>'")) { getClassToEnumFooInline() }
    expectFailure(linkage("Constructor 'Foo.<init>' can not be called: No constructor found for symbol '/ClassToEnum.Foo.<init>'")) { getClassToEnumFooAsAnyInline() }
    expectFailure(linkage("Can not get instance of singleton 'Bar': No class found for symbol '/ClassToEnum.Bar'")) { getClassToEnumBarInline() }
    expectFailure(linkage("Can not get instance of singleton 'Bar': No class found for symbol '/ClassToEnum.Bar'")) { getClassToEnumBarAsAnyInline() }
    expectFailure(linkage("Constructor 'ClassToEnum.<init>' can not be called: Private constructor declared in module <lib1> can not be accessed in module <main>")) { getClassToEnumBazInline() }
    expectFailure(linkage("Constructor 'ClassToEnum.<init>' can not be called: Private constructor declared in module <lib1> can not be accessed in module <main>")) { getClassToEnumBazAsAnyInline() }
    expectFailure(linkage("Constructor 'Foo.<init>' can not be called: No constructor found for symbol '/ObjectToEnum.Foo.<init>'")) { getObjectToEnumFooInline() }
    expectFailure(linkage("Constructor 'Foo.<init>' can not be called: No constructor found for symbol '/ObjectToEnum.Foo.<init>'")) { getObjectToEnumFooAsAnyInline() }
    expectFailure(linkage("Can not get instance of singleton 'Bar': No class found for symbol '/ObjectToEnum.Bar'")) { getObjectToEnumBarInline() }
    expectFailure(linkage("Can not get instance of singleton 'Bar': No class found for symbol '/ObjectToEnum.Bar'")) { getObjectToEnumBarAsAnyInline() }

    expectFailure(linkage("Can not get instance of singleton 'EnumToClass.Foo': No enum entry found for symbol '/EnumToClass.Foo'")) { getEnumToClassFooInline() }
    expectFailure(linkage("Can not get instance of singleton 'EnumToClass.Foo': No enum entry found for symbol '/EnumToClass.Foo'")) { getEnumToClassFooAsAnyInline() }
    expectFailure(linkage("Can not get instance of singleton 'EnumToClass.Bar': No enum entry found for symbol '/EnumToClass.Bar'")) { getEnumToClassBarInline() }
    expectFailure(linkage("Can not get instance of singleton 'EnumToClass.Bar': No enum entry found for symbol '/EnumToClass.Bar'")) { getEnumToClassBarAsAnyInline() }
    expectFailure(linkage("Can not get instance of singleton 'EnumToClass.Baz': No enum entry found for symbol '/EnumToClass.Baz'")) { getEnumToClassBazInline() }
    expectFailure(linkage("Can not get instance of singleton 'EnumToClass.Baz': No enum entry found for symbol '/EnumToClass.Baz'")) { getEnumToClassBazAsAnyInline() }

    expectFailure(linkage("Can not get instance of singleton 'EnumToObject.Foo': No enum entry found for symbol '/EnumToObject.Foo'")) { getEnumToObjectFooInline() }
    expectFailure(linkage("Can not get instance of singleton 'EnumToObject.Foo': No enum entry found for symbol '/EnumToObject.Foo'")) { getEnumToObjectFooAsAnyInline() }
    expectFailure(linkage("Can not get instance of singleton 'EnumToObject.Bar': No enum entry found for symbol '/EnumToObject.Bar'")) { getEnumToObjectBarInline() }
    expectFailure(linkage("Can not get instance of singleton 'EnumToObject.Bar': No enum entry found for symbol '/EnumToObject.Bar'")) { getEnumToObjectBarAsAnyInline() }

    expectFailure(linkage("Constructor 'ClassToObject.<init>' can not be called: Private constructor declared in module <lib1> can not be accessed in module <main>")) { getClassToObjectInline() }
    expectFailure(linkage("Constructor 'ClassToObject.<init>' can not be called: Private constructor declared in module <lib1> can not be accessed in module <main>")) { getClassToObjectAsAnyInline() }

    expectFailure(linkage("Can not get instance of singleton 'ObjectToClass': 'ObjectToClass' is class while object is expected")) { getObjectToClassInline() }
    expectFailure(linkage("Can not get instance of singleton 'ObjectToClass': 'ObjectToClass' is class while object is expected")) { getObjectToClassAsAnyInline() }

    expectFailure(linkage("Constructor 'ClassToInterface.<init>' can not be called: No constructor found for symbol '/ClassToInterface.<init>'")) { getClassToInterfaceInline() }
    expectFailure(linkage("Constructor 'ClassToInterface.<init>' can not be called: No constructor found for symbol '/ClassToInterface.<init>'")) { getClassToInterfaceAsAnyInline() }

    expectSuccess("NestedObjectToCompanion1.Companion") { getNestedObjectToCompanion1Inline().toString() }
    expectSuccess("NestedObjectToCompanion1.Companion") { getNestedObjectToCompanion1AsAnyInline().toString() }

    expectSuccess("NestedObjectToCompanion2.Foo") { getNestedObjectToCompanion2Inline().toString() }
    expectSuccess("NestedObjectToCompanion2.Foo") { getNestedObjectToCompanion2AsAnyInline().toString() }

    expectSuccess("CompanionToNestedObject1.Companion") { getCompanionToNestedObject1Inline().toString() }
    expectSuccess("CompanionToNestedObject1.Companion") { getCompanionToNestedObject1AsAnyInline().toString() }
    expectSuccess("CompanionToNestedObject1.Companion") { getCompanionToNestedObject1NameInline() }
    expectSuccess("CompanionToNestedObject1.Companion") { getCompanionToNestedObject1NameShortInline() }

    expectSuccess("CompanionToNestedObject2.Foo") { getCompanionToNestedObject2Inline().toString() }
    expectSuccess("CompanionToNestedObject2.Foo") { getCompanionToNestedObject2AsAnyInline().toString() }
    expectSuccess("CompanionToNestedObject2.Foo") { getCompanionToNestedObject2NameInline() }
    expectSuccess("CompanionToNestedObject2.Foo") { getCompanionToNestedObject2NameShortInline() }

    expectSuccess("Foo") { getCompanionAndNestedObjectsSwapInline() }

    expectFailure(linkage("Constructor 'NestedToInner.<init>' can not be called: The call site has 1 less value argument(s) than the constructor requires. Those arguments are missing: <this>")) { getNestedToInnerNameInline() }
    expectFailure(linkage("Can not get instance of singleton 'Object': 'Object' is inner class while object is expected")) { getNestedToInnerObjectNameInline() }
    expectFailure(linkage("Can not get instance of singleton 'Companion': 'Companion' is inner class while object is expected")) { getNestedToInnerCompanionNameInline() }
    expectFailure(linkage("Constructor 'Nested.<init>' can not be called: The call site has 1 less value argument(s) than the constructor requires. Those arguments are missing: <this>")) { getNestedToInnerNestedNameInline() }
    expectFailure(linkage("Constructor 'NestedToInner.<init>' can not be called: The call site has 1 less value argument(s) than the constructor requires. Those arguments are missing: <this>")) { getNestedToInnerInnerNameInline() }

    expectFailure(linkage("Constructor 'InnerToNested.<init>' can not be called: The call site provides 1 more value argument(s) than the constructor expects")) { getInnerToNestedNameInline() }
    expectFailure(linkage("Constructor 'InnerToNested.<init>' can not be called: The call site provides 1 more value argument(s) than the constructor expects")) { getInnerToNestedObjectNameInline() }
    expectFailure(linkage("Constructor 'InnerToNested.<init>' can not be called: The call site provides 1 more value argument(s) than the constructor expects")) { getInnerToNestedCompanionNameInline() }
    expectFailure(linkage("Constructor 'InnerToNested.<init>' can not be called: The call site provides 1 more value argument(s) than the constructor expects")) { getInnerToNestedNestedNameInline() }
    expectFailure(linkage("Constructor 'InnerToNested.<init>' can not be called: The call site provides 1 more value argument(s) than the constructor expects")) { getInnerToNestedInnerNameInline() }

    expectFailure(linkage("Constructor 'AnnotationClassWithChangedParameterType.<init>' can not be called: No constructor found for symbol '/AnnotationClassWithChangedParameterType.<init>'")) { getAnnotationClassWithChangedParameterTypeInline() }
    expectFailure(linkage("Constructor 'AnnotationClassWithChangedParameterType.<init>' can not be called: No constructor found for symbol '/AnnotationClassWithChangedParameterType.<init>'")) { getAnnotationClassWithChangedParameterTypeAsAnyInline() }
    expectSuccess(106) { getAnnotationClassThatBecomesRegularClassInline().x }
    expectSuccess("AnnotationClassThatBecomesRegularClass[x=108]") { getAnnotationClassThatBecomesRegularClassAsAnyInline().toString() }
    expectFailure(linkage("Constructor 'AnnotationClassWithParameterThatBecomesRegularClass.<init>' can not be called: Annotation class 'AnnotationClassWithParameterThatBecomesRegularClass' uses non-annotation class 'AnnotationClassThatBecomesRegularClass' as a parameter")) { getAnnotationClassWithParameterThatBecomesRegularClassInline().x.x }
    expectFailure(linkage("Constructor 'AnnotationClassWithParameterThatBecomesRegularClass.<init>' can not be called: Annotation class 'AnnotationClassWithParameterThatBecomesRegularClass' uses non-annotation class 'AnnotationClassThatBecomesRegularClass' as a parameter")) { getAnnotationClassWithParameterThatBecomesRegularClassAsAnyInline().toString() }
    expectFailure(linkage("Constructor 'AnnotationClassWithParameterThatBecomesRegularClass.<init>' can not be called: Annotation class 'AnnotationClassWithParameterThatBecomesRegularClass' uses non-annotation class 'AnnotationClassThatBecomesRegularClass' as a parameter")) { getAnnotationClassWithParameterOfParameterThatBecomesRegularClassInline().x.x.x }
    expectFailure(linkage("Constructor 'AnnotationClassWithParameterThatBecomesRegularClass.<init>' can not be called: Annotation class 'AnnotationClassWithParameterThatBecomesRegularClass' uses non-annotation class 'AnnotationClassThatBecomesRegularClass' as a parameter")) { getAnnotationClassWithParameterOfParameterThatBecomesRegularClassAsAnyInline().toString() }
    expectFailure(linkage("Constructor 'AnnotationClassThatDisappears.<init>' can not be called: No constructor found for symbol '/AnnotationClassThatDisappears.<init>'")) { getAnnotationClassThatDisappearsInline() }
    expectFailure(linkage("Constructor 'AnnotationClassThatDisappears.<init>' can not be called: No constructor found for symbol '/AnnotationClassThatDisappears.<init>'")) { getAnnotationClassThatDisappearsAsAnyInline() }
    expectFailure(linkage("Constructor 'AnnotationClassThatDisappears.<init>' can not be called: No constructor found for symbol '/AnnotationClassThatDisappears.<init>'")) { getAnnotationClassWithParameterThatDisappearsInline() }
    expectFailure(linkage("Constructor 'AnnotationClassThatDisappears.<init>' can not be called: No constructor found for symbol '/AnnotationClassThatDisappears.<init>'")) { getAnnotationClassWithParameterOfParameterThatDisappearsInline() }
    expectFailure(linkage("Constructor 'AnnotationClassThatDisappears.<init>' can not be called: No constructor found for symbol '/AnnotationClassThatDisappears.<init>'")) { getAnnotationClassWithParameterThatDisappearsAsAnyInline() }
    if (!testMode.isWasm) { //toString for annotation classes are not supported KT-66385
        expectSuccess("@AnnotationClassWithRenamedParameters(xi=130, xs=Pear)") { getAnnotationClassWithRenamedParametersInline().toString() }
        expectSuccess("@AnnotationClassWithRenamedParameters(xi=132, xs=Peach)") { getAnnotationClassWithRenamedParametersAsAnyInline().toString() }
    }
    expectFailure(linkage("Constructor 'AnnotationClassWithReorderedParameters.<init>' can not be called: No constructor found for symbol '/AnnotationClassWithReorderedParameters.<init>'")) { getAnnotationClassWithReorderedParametersInline() }
    expectFailure(linkage("Constructor 'AnnotationClassWithReorderedParameters.<init>' can not be called: No constructor found for symbol '/AnnotationClassWithReorderedParameters.<init>'")) { getAnnotationClassWithReorderedParametersAsAnyInline() }
    expectFailure(linkage("Constructor 'AnnotationClassWithNewParameter.<init>' can not be called: No constructor found for symbol '/AnnotationClassWithNewParameter.<init>'")) { getAnnotationClassWithNewParameterInline() }
    expectFailure(linkage("Constructor 'AnnotationClassWithNewParameter.<init>' can not be called: No constructor found for symbol '/AnnotationClassWithNewParameter.<init>'")) { getAnnotationClassWithNewParameterAsAnyInline() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithClassReferenceParameterThatDisappears1Inline() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithClassReferenceParameterThatDisappears1AsAnyInline() }
    expectFailure(linkage("Constructor 'AnnotationClassWithClassReferenceParameterThatDisappears1.<init>' can not be called: Constructor uses unlinked class symbol '/RemovedClass'")) { getAnnotationClassWithDefaultClassReferenceParameterThatDisappears1Inline() }
    expectFailure(linkage("Constructor 'AnnotationClassWithClassReferenceParameterThatDisappears1.<init>' can not be called: Constructor uses unlinked class symbol '/RemovedClass'")) { getAnnotationClassWithDefaultClassReferenceParameterThatDisappears1AsAnyInline() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithClassReferenceParameterThatDisappears2Inline() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithClassReferenceParameterThatDisappears2AsAnyInline() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithDefaultClassReferenceParameterThatDisappears2Inline() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithDefaultClassReferenceParameterThatDisappears2AsAnyInline() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithClassReferenceParameterOfParameterThatDisappears1Inline() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithClassReferenceParameterOfParameterThatDisappears1AsAnyInline() }
    expectFailure(linkage("Constructor 'AnnotationClassWithClassReferenceParameterOfParameterThatDisappears1.<init>' can not be called: Constructor uses unlinked class symbol '/RemovedClass' (via annotation class 'AnnotationClassWithClassReferenceParameterThatDisappears1')")) { getAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears1Inline() }
    expectFailure(linkage("Constructor 'AnnotationClassWithClassReferenceParameterOfParameterThatDisappears1.<init>' can not be called: Constructor uses unlinked class symbol '/RemovedClass' (via annotation class 'AnnotationClassWithClassReferenceParameterThatDisappears1')")) { getAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears1AsAnyInline() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithClassReferenceParameterOfParameterThatDisappears2Inline() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithClassReferenceParameterOfParameterThatDisappears2AsAnyInline() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears2Inline() }
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { getAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears2AsAnyInline() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassWithDisappearingEntry.REMOVED': No enum entry found for symbol '/EnumClassWithDisappearingEntry.REMOVED'")) { getAnnotationClassWithRemovedEnumEntryParameterInline() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassWithDisappearingEntry.REMOVED': No enum entry found for symbol '/EnumClassWithDisappearingEntry.REMOVED'")) { getAnnotationClassWithRemovedEnumEntryParameterAsAnyInline() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassWithDisappearingEntry.REMOVED': No enum entry found for symbol '/EnumClassWithDisappearingEntry.REMOVED'")) { getAnnotationClassWithDefaultRemovedEnumEntryParameterInline() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassWithDisappearingEntry.REMOVED': No enum entry found for symbol '/EnumClassWithDisappearingEntry.REMOVED'")) { getAnnotationClassWithDefaultRemovedEnumEntryParameterAsAnyInline() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassWithDisappearingEntry.REMOVED': No enum entry found for symbol '/EnumClassWithDisappearingEntry.REMOVED'")) { getAnnotationClassWithRemovedEnumEntryParameterOfParameterInline() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassWithDisappearingEntry.REMOVED': No enum entry found for symbol '/EnumClassWithDisappearingEntry.REMOVED'")) { getAnnotationClassWithRemovedEnumEntryParameterOfParameterAsAnyInline() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassWithDisappearingEntry.REMOVED': No enum entry found for symbol '/EnumClassWithDisappearingEntry.REMOVED'")) { getAnnotationClassWithDefaultRemovedEnumEntryParameterOfParameterInline() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassWithDisappearingEntry.REMOVED': No enum entry found for symbol '/EnumClassWithDisappearingEntry.REMOVED'")) { getAnnotationClassWithDefaultRemovedEnumEntryParameterOfParameterAsAnyInline() }
    expectFailure(linkage("Constructor 'AnnotationClassThatBecomesPrivate.<init>' can not be called: Private constructor declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterThatBecomesPrivate1Inline() }
    expectFailure(linkage("Constructor 'AnnotationClassThatBecomesPrivate.<init>' can not be called: Private constructor declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterThatBecomesPrivate1AsAnyInline() }
    expectFailure(linkage("Reference to class 'ClassThatBecomesPrivate' can not be evaluated: Private class declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterThatBecomesPrivate2Inline() }
    expectFailure(linkage("Reference to class 'ClassThatBecomesPrivate' can not be evaluated: Private class declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterThatBecomesPrivate2AsAnyInline() }
    expectFailure(linkage("Reference to class 'ClassThatBecomesPrivate' can not be evaluated: Private class declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterOfParameterThatBecomesPrivate2Inline() }
    expectFailure(linkage("Reference to class 'ClassThatBecomesPrivate' can not be evaluated: Private class declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterOfParameterThatBecomesPrivate2AsAnyInline() }
    expectFailure(linkage("Reference to class 'ClassThatBecomesPrivate' can not be evaluated: Private class declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterThatBecomesPrivate3Inline() }
    expectFailure(linkage("Reference to class 'ClassThatBecomesPrivate' can not be evaluated: Private class declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterThatBecomesPrivate3AsAnyInline() }
    expectFailure(linkage("Reference to class 'ClassThatBecomesPrivate' can not be evaluated: Private class declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterOfParameterThatBecomesPrivate3Inline() }
    expectFailure(linkage("Reference to class 'ClassThatBecomesPrivate' can not be evaluated: Private class declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterOfParameterThatBecomesPrivate3AsAnyInline() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassThatBecomesPrivate.ENTRY': Private enum entry declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterThatBecomesPrivate4Inline().toString() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassThatBecomesPrivate.ENTRY': Private enum entry declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterThatBecomesPrivate4AsAnyInline().toString() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassThatBecomesPrivate.ENTRY': Private enum entry declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterOfParameterThatBecomesPrivate4Inline().toString() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassThatBecomesPrivate.ENTRY': Private enum entry declared in module <lib1> can not be accessed in module <lib2>")) { getAnnotationClassWithParameterOfParameterThatBecomesPrivate4AsAnyInline().toString() }
    expectSuccess { getAnnotationClassWithParameterWithPrivateDefaultValueInline(); "OK" }
    expectSuccess { getAnnotationClassWithParameterWithPrivateDefaultValueInlineAsAny(); "OK" }
    expectSuccess { getAnnotationClassWithParameterOfParameterWithPrivateDefaultValueInline(); "OK" }
    expectSuccess { getAnnotationClassWithParameterOfParameterWithPrivateDefaultValueInlineAsAny(); "OK" }

    // Handle unlinked constructor call in annotation & non-annotation class appearing in annotation:
    expectSuccess("HolderOfAnnotationClassWithChangedParameterType") { getHolderOfAnnotationClassWithChangedParameterTypeInline().toString() }
    expectSuccess("HolderOfAnnotationClassThatBecomesRegularClass") { getHolderOfAnnotationClassThatBecomesRegularClassInline().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterThatBecomesRegularClass") { getHolderOfAnnotationClassWithParameterThatBecomesRegularClassInline().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterOfParameterThatBecomesRegularClass") { getHolderOfAnnotationClassWithParameterOfParameterThatBecomesRegularClassInline().toString() }
    expectSuccess("HolderOfAnnotationClassThatDisappears") { getHolderOfAnnotationClassThatDisappearsInline().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterThatDisappears") { getHolderOfAnnotationClassWithParameterThatDisappearsInline().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterOfParameterThatDisappears") { getHolderOfAnnotationClassWithParameterOfParameterThatDisappearsInline().toString() }
    expectSuccess("HolderOfAnnotationClassWithRenamedParameters") { getHolderOfAnnotationClassWithRenamedParametersInline().toString() }
    expectSuccess("HolderOfAnnotationClassWithReorderedParameters") { getHolderOfAnnotationClassWithReorderedParametersInline().toString() }
    expectSuccess("HolderOfAnnotationClassWithNewParameter") { getHolderOfAnnotationClassWithNewParameterInline().toString() }
    expectSuccess("HolderOfAnnotationClassWithClassReferenceParameterThatDisappears1") { getHolderOfAnnotationClassWithClassReferenceParameterThatDisappears1Inline().toString() }
    expectSuccess("HolderOfAnnotationClassWithDefaultClassReferenceParameterThatDisappears1") { getHolderOfAnnotationClassWithDefaultClassReferenceParameterThatDisappears1Inline().toString() }
    expectSuccess("HolderOfAnnotationClassWithClassReferenceParameterThatDisappears2") { getHolderOfAnnotationClassWithClassReferenceParameterThatDisappears2Inline().toString() }
    expectSuccess("HolderOfAnnotationClassWithDefaultClassReferenceParameterThatDisappears2") { getHolderOfAnnotationClassWithDefaultClassReferenceParameterThatDisappears2Inline().toString() }
    expectSuccess("HolderOfAnnotationClassWithClassReferenceParameterOfParameterThatDisappears1") { getHolderOfAnnotationClassWithClassReferenceParameterOfParameterThatDisappears1Inline().toString() }
    expectSuccess("HolderOfAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears1") { getHolderOfAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears1Inline().toString() }
    expectSuccess("HolderOfAnnotationClassWithClassReferenceParameterOfParameterThatDisappears2") { getHolderOfAnnotationClassWithClassReferenceParameterOfParameterThatDisappears2Inline().toString() }
    expectSuccess("HolderOfAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears2") { getHolderOfAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears2Inline().toString() }
    expectSuccess("HolderOfAnnotationClassWithRemovedEnumEntryParameter") { getHolderOfAnnotationClassWithRemovedEnumEntryParameterInline().toString() }
    expectSuccess("HolderOfAnnotationClassWithDefaultRemovedEnumEntryParameter") { getHolderOfAnnotationClassWithDefaultRemovedEnumEntryParameterInline().toString() }
    expectSuccess("HolderOfAnnotationClassWithRemovedEnumEntryParameterOfParameter") { getHolderOfAnnotationClassWithRemovedEnumEntryParameterOfParameterInline().toString() }
    expectSuccess("HolderOfAnnotationClassWithDefaultRemovedEnumEntryParameterOfParameter") { getHolderOfAnnotationClassWithDefaultRemovedEnumEntryParameterOfParameterInline().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterThatBecomesPrivate1") { getHolderOfAnnotationClassWithParameterThatBecomesPrivate1Inline().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterThatBecomesPrivate2") { getHolderOfAnnotationClassWithParameterThatBecomesPrivate2Inline().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate2") { getHolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate2Inline().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterThatBecomesPrivate3") { getHolderOfAnnotationClassWithParameterThatBecomesPrivate3Inline().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate3") { getHolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate3Inline().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterThatBecomesPrivate4") { getHolderOfAnnotationClassWithParameterThatBecomesPrivate4Inline().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate4") { getHolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate4Inline().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterWithPrivateDefaultValue") { getHolderOfAnnotationClassWithParameterWithPrivateDefaultValueInline().toString() }
    expectSuccess("HolderOfAnnotationClassWithParameterOfParameterWithPrivateDefaultValue") { getHolderOfAnnotationClassWithParameterOfParameterWithPrivateDefaultValueInline().toString() }

    expectSuccess { getValueToClassInline(); "OK" }
    expectSuccess { getValueToClassAsAnyInline(); "OK" }

    expectSuccess { getClassToValueInline(); "OK" }
    expectSuccess { getClassToValueAsAnyInline(); "OK" }


    /*****************************************************/
    /***** Extracted from 'functionTransformations': *****/
    /*****************************************************/

    val oci: OpenClassImpl = OpenClassImpl()
    val oc: OpenClass = oci

    expectSuccess("OpenClassV2.openNonInlineToInlineFunction(1)") { openNonInlineToInlineFunctionInOpenClass(oc, 1) }
    expectSuccess("OpenClassV2.openNonInlineToInlineFunctionWithDelegation(2)") { openNonInlineToInlineFunctionWithDelegationInOpenClass(oc, 2) }
    expectSuccess("OpenClassV2.newInlineFunction1(3)") { newInlineFunction1InOpenClass(oc, 3) }
    expectSuccess("OpenClassV2.newInlineFunction2(4)") { newInlineFunction2InOpenClass(oc, 4) }
    expectSuccess( // TODO: this should be fixed in JS, KT-56762
        if (testMode.isJs) "OpenClassImpl.newNonInlineFunction(5)" else "OpenClassV2.newNonInlineFunction(5)"
    ) { newNonInlineFunctionInOpenClass(oc, 5) }
    expectSuccess("OpenClassImpl.openNonInlineToInlineFunction(6)") { openNonInlineToInlineFunctionInOpenClassImpl(oci, 6) }
    expectSuccess("OpenClassV2.openNonInlineToInlineFunctionWithDelegation(7) called from OpenClassImpl.openNonInlineToInlineFunctionWithDelegation(7)") { openNonInlineToInlineFunctionWithDelegationInOpenClassImpl(oci, 7) }
    expectSuccess("OpenClassImpl.newInlineFunction1(8)") { newInlineFunction1InOpenClassImpl(oci, 8) }
    expectSuccess("OpenClassImpl.newInlineFunction2(9)") { newInlineFunction2InOpenClassImpl(oci, 9) }
    expectSuccess("OpenClassImpl.newNonInlineFunction(10)") { newNonInlineFunctionInOpenClassImpl(oci, 10) }

    expectSuccess("Functions.inlineLambdaToNoinlineLambda(3) { 6 }") { inlineLambdaToNoinlineLambda(3) }
    expectSuccess("inlineLambdaToNoinlineLambda(-3)") { inlineLambdaToNoinlineLambda(-3) }
    expectSuccess("Functions.inlineLambdaToCrossinlineLambda(5) { 10 }") { inlineLambdaToCrossinlineLambda(5) }
    expectSuccess("inlineLambdaToCrossinlineLambda(-5)") { inlineLambdaToCrossinlineLambda(-5) }

    expectSuccess(-3) { suspendToNonSuspendFunction3(3) }
    expectFailure(linkage("Function 'nonSuspendToSuspendFunction' can not be called: Suspend function can be called only from a coroutine or another suspend function")) { nonSuspendToSuspendFunction3(6) }
    expectSuccess(-7) { nonSuspendToSuspendFunction4(7) }

    /********************************************/
    /***** Extracted from 'removeCallable': *****/
    /********************************************/

    expectFailure(linkage("Function 'removedFunction' can not be called: No function found for symbol '/removedFunction'")) { callInlinedRemovedFunction() }
    expectFailure(linkage("Property accessor 'removedProperty.<get-removedProperty>' can not be called: No property accessor found for symbol '/removedProperty.<get-removedProperty>'")) { readInlinedRemovedProperty() }

    /*****************************************/
    /***** Extracted from 'removeClass': *****/
    /*****************************************/

    expectFailure(linkage("Can not read value from variable 'foo': Variable uses unlinked class symbol '/RemovedOpenClass'")) { inlinedFunctionWithRemovedOpenClassVariableType() }
    expectFailure(linkage("Can not read value from variable 'foo': Variable uses unlinked class symbol '/RemovedOpenClass' (via class 'OpenClassInheritedFromRemovedOpenClass')")) { inlinedFunctionWithOpenClassImplVariableType() }
    expectFailure(linkage("Constructor 'RemovedOpenClass.<init>' can not be called: No constructor found for symbol '/RemovedOpenClass.<init>'")) { inlinedFunctionWithCreationOfRemovedOpenClass() }
    expectFailure(linkage("Constructor 'OpenClassInheritedFromRemovedOpenClass.<init>' can not be called: Class 'OpenClassInheritedFromRemovedOpenClass' uses unlinked class symbol '/RemovedOpenClass'")) { inlinedFunctionWithCreationOfOpenClassImpl() }
    expectFailure(linkage("Reference to constructor 'RemovedOpenClass.<init>' can not be evaluated: Expression uses unlinked class symbol '/RemovedOpenClass'")) { inlinedFunctionWithCreationOfRemovedOpenClassThroughReference() }
    expectFailure(linkage("Reference to constructor 'OpenClassInheritedFromRemovedOpenClass.<init>' can not be evaluated: Expression uses unlinked class symbol '/RemovedOpenClass' (via class 'OpenClassInheritedFromRemovedOpenClass')")) { inlinedFunctionWithCreationOfOpenClassImplThroughReference() }
    expectFailure(linkage("Can not read value from variable 'foo': Variable uses unlinked class symbol '/RemovedOpenClass' (via anonymous object)")) { inlinedFunctionWithRemovedOpenClassAnonymousObject() }
    expectFailure(linkage("Can not read value from variable 'foo': Variable uses unlinked class symbol '/RemovedOpenClass' (via anonymous object)")) { inlinedFunctionWithOpenClassImplAnonymousObject() }

    /***********************************************/
    /***** Extracted from 'inheritanceIssues': *****/
    /***********************************************/

    expectFailure(linkage("Anonymous object initialization error: Constructor '<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassInline() }
    expectFailure(linkage("Anonymous object initialization error: Constructor '<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassAsAnyInline() }

    expectFailure(linkage("Anonymous object initialization error: Constructor '<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassInline() }
    expectFailure(linkage("Anonymous object initialization error: Constructor '<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassAsAnyInline() }

    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassInline() }
    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassAsAnyInline() }

    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToAbstractClassImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassImplInline() }
    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToAbstractClassImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassImplAsAnyInline() }

    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToAbstractClassImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassImpl2Inline() }
    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToAbstractClassImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassImpl2AsAnyInline() }

    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToOpenClassImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassImplInline() }
    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToOpenClassImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassImplAsAnyInline() }

    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToOpenClassImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassImpl2Inline() }
    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToOpenClassImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassImpl2AsAnyInline() }

    expectFailure(linkage("Constructor 'InterfaceToFinalClassImpl.<init>' can not be called: Class 'InterfaceToFinalClassImpl' inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassImplInline() }
    expectFailure(linkage("Constructor 'InterfaceToFinalClassImpl.<init>' can not be called: Class 'InterfaceToFinalClassImpl' inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassImplAsAnyInline() }

    expectFailure(linkage("Constructor 'InterfaceToFinalClassImpl2.<init>' can not be called: Class 'InterfaceToFinalClassImpl2' uses class 'InterfaceToFinalClassImpl' that inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassImpl2Inline() }
    expectFailure(linkage("Constructor 'InterfaceToFinalClassImpl2.<init>' can not be called: Class 'InterfaceToFinalClassImpl2' uses class 'InterfaceToFinalClassImpl' that inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassImpl2AsAnyInline() }

    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToAbstractClassImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassNestedImplInline() }
    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToAbstractClassImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassNestedImplAsAnyInline() }

    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToAbstractClassImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassNestedImpl2Inline() }
    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToAbstractClassImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassNestedImpl2AsAnyInline() }

    expectFailure(linkage("Inner class initialization error: Constructor 'InterfaceToAbstractClassInnerImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassInnerImplInline() }
    expectFailure(linkage("Inner class initialization error: Constructor 'InterfaceToAbstractClassInnerImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassInnerImplAsAnyInline() }

    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToOpenClassImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassNestedImplInline() }
    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToOpenClassImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassNestedImplAsAnyInline() }

    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToOpenClassImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassNestedImpl2Inline() }
    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToOpenClassImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassNestedImpl2AsAnyInline() }

    expectFailure(linkage("Inner class initialization error: Constructor 'InterfaceToOpenClassInnerImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassInnerImplInline() }
    expectFailure(linkage("Inner class initialization error: Constructor 'InterfaceToOpenClassInnerImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassInnerImplAsAnyInline() }

    expectFailure(linkage("Constructor 'InterfaceToFinalClassImpl.<init>' can not be called: Class 'InterfaceToFinalClassImpl' inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassNestedImplInline() }
    expectFailure(linkage("Constructor 'InterfaceToFinalClassImpl.<init>' can not be called: Class 'InterfaceToFinalClassImpl' inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassNestedImplAsAnyInline() }

    expectFailure(linkage("Constructor 'InterfaceToFinalClassImpl2.<init>' can not be called: Class 'InterfaceToFinalClassImpl2' uses class 'InterfaceToFinalClassImpl' that inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassNestedImpl2Inline() }
    expectFailure(linkage("Constructor 'InterfaceToFinalClassImpl2.<init>' can not be called: Class 'InterfaceToFinalClassImpl2' uses class 'InterfaceToFinalClassImpl' that inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassNestedImpl2AsAnyInline() }

    expectFailure(linkage("Constructor 'InterfaceToFinalClassInnerImpl.<init>' can not be called: Inner class 'InterfaceToFinalClassInnerImpl' inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassInnerImplInline() }
    expectFailure(linkage("Constructor 'InterfaceToFinalClassInnerImpl.<init>' can not be called: Inner class 'InterfaceToFinalClassInnerImpl' inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassInnerImplAsAnyInline() }

    expectSuccess("InterfaceToAbstractClassImpl") { referenceToInterfaceToAbstractClassImplInline() }
    expectSuccess("InterfaceToAbstractClassImpl2") { referenceToInterfaceToAbstractClassImpl2Inline() }

    expectFailure(linkage("Reference to class 'InterfaceToFinalClassImpl' can not be evaluated: Expression uses class 'InterfaceToFinalClassImpl' that inherits from final class 'InterfaceToFinalClass'")) { referenceToInterfaceToFinalClassImplInline() }
    expectFailure(linkage("Reference to class 'InterfaceToFinalClassImpl2' can not be evaluated: Expression uses class 'InterfaceToFinalClassImpl' (via class 'InterfaceToFinalClassImpl2') that inherits from final class 'InterfaceToFinalClass'")) { referenceToInterfaceToFinalClassImpl2Inline() }

    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { getInterfaceToAbstractClass12_1Inline() }
    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { getInterfaceToAbstractClass12_2Inline() }
    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { getInterfaceToAbstractClass12AsAnyInline() }

    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { getInterfaceToAbstractClassAndAbstractClass_1Inline() }
    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { getInterfaceToAbstractClassAndAbstractClass_2Inline() }
    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { getInterfaceToAbstractClassAndAbstractClassAsAnyInline() }

    expectFailure(linkage("Constructor 'InterfaceToAbstractClass12Impl.<init>' can not be called: Class 'InterfaceToAbstractClass12Impl' simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { getInterfaceToAbstractClass12ImplInline() }
    expectFailure(linkage("Constructor 'InterfaceToAbstractClass12Impl.<init>' can not be called: Class 'InterfaceToAbstractClass12Impl' simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { getInterfaceToAbstractClass12ImplAsAnyInline() }

    expectFailure(linkage("Constructor 'InterfaceToAbstractClass12Impl2.<init>' can not be called: Class 'InterfaceToAbstractClass12Impl2' uses class 'InterfaceToAbstractClass12Impl' that simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { getInterfaceToAbstractClass12Impl2Inline() }
    expectFailure(linkage("Constructor 'InterfaceToAbstractClass12Impl2.<init>' can not be called: Class 'InterfaceToAbstractClass12Impl2' uses class 'InterfaceToAbstractClass12Impl' that simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { getInterfaceToAbstractClass12Impl2AsAnyInline() }

    expectFailure(linkage("Constructor 'InterfaceToAbstractClassAndAbstractClassImpl.<init>' can not be called: Class 'InterfaceToAbstractClassAndAbstractClassImpl' simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { getInterfaceToAbstractClassAndAbstractClassImplInline() }
    expectFailure(linkage("Constructor 'InterfaceToAbstractClassAndAbstractClassImpl.<init>' can not be called: Class 'InterfaceToAbstractClassAndAbstractClassImpl' simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { getInterfaceToAbstractClassAndAbstractClassImplAsAnyInline() }

    expectFailure(linkage("Constructor 'InterfaceToAbstractClassAndAbstractClassImpl2.<init>' can not be called: Class 'InterfaceToAbstractClassAndAbstractClassImpl2' uses class 'InterfaceToAbstractClassAndAbstractClassImpl' that simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { getInterfaceToAbstractClassAndAbstractClassImpl2Inline() }
    expectFailure(linkage("Constructor 'InterfaceToAbstractClassAndAbstractClassImpl2.<init>' can not be called: Class 'InterfaceToAbstractClassAndAbstractClassImpl2' uses class 'InterfaceToAbstractClassAndAbstractClassImpl' that simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { getInterfaceToAbstractClassAndAbstractClassImpl2AsAnyInline() }

    expectFailure(linkage("Reference to class 'InterfaceToAbstractClass12Impl' can not be evaluated: Expression uses class 'InterfaceToAbstractClass12Impl' that simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { referenceToInterfaceToAbstractClass12ImplInline() }
    expectFailure(linkage("Reference to class 'InterfaceToAbstractClass12Impl2' can not be evaluated: Expression uses class 'InterfaceToAbstractClass12Impl' (via class 'InterfaceToAbstractClass12Impl2') that simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { referenceToInterfaceToAbstractClass12Impl2Inline() }
    expectFailure(linkage("Reference to class 'InterfaceToAbstractClassAndAbstractClassImpl' can not be evaluated: Expression uses class 'InterfaceToAbstractClassAndAbstractClassImpl' that simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { referenceToInterfaceToAbstractClassAndAbstractClassImplInline() }
    expectFailure(linkage("Reference to class 'InterfaceToAbstractClassAndAbstractClassImpl2' can not be evaluated: Expression uses class 'InterfaceToAbstractClassAndAbstractClassImpl' (via class 'InterfaceToAbstractClassAndAbstractClassImpl2') that simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { referenceToInterfaceToAbstractClassAndAbstractClassImpl2Inline() }

    /*************************************/
    /***** Extracted from 'kt73511': *****/
    /*************************************/

    expectSuccess("MyAnnotationHolder(x=42)") { createMyAnnotationHolderInstance(42).toString() }

    /******************************************************/
    /***** Extracted from 'richReferencesOperations': *****/
    /******************************************************/

    // inline fun
    expectSuccess(true) { createRemovedInlineFunReference() is kotlin.reflect.KFunction<*> }
    expectSuccess("removedInlineFun") { removedInlineFunReferenceName() }
    expectSuccess(123) { removedInlineFunReferenceInvoke() }

    if (!testMode.isJs) {
        expectFailure(linkage("Reference to function 'removedInlineFun' can not be evaluated: No function found for symbol '/removedInlineFun'")) { removedInlineFunReferenceHashCode() }
        expectFailure(linkage("Reference to function 'removedInlineFun' can not be evaluated: No function found for symbol '/removedInlineFun'")) { removedInlineFunReferenceEquals() }
        expectFailure(linkage("Reference to function 'removedInlineFun' can not be evaluated: No function found for symbol '/removedInlineFun'")) { removedInlineFunReferenceToString() }
    }

    // inline val
    expectSuccess(true) { createRemovedInlineValReference() is kotlin.reflect.KProperty0<*> }
    expectFailure(linkage("Reference to property 'removedInlineVal' can not be evaluated: No property found for symbol '/removedInlineVal'")) { removedInlineValReferenceName() }
    expectSuccess(321) { removedInlineValReferenceInvoke() }
    expectSuccess(321) { removedInlineValReferenceGet() }

    if (!testMode.isJs) {
        expectFailure(linkage("Reference to property 'removedInlineVal' can not be evaluated: No property found for symbol '/removedInlineVal'")) { removedInlineValReferenceHashCode() }
        expectFailure(linkage("Reference to property 'removedInlineVal' can not be evaluated: No property found for symbol '/removedInlineVal'")) { removedInlineValReferenceEquals() }
        expectFailure(linkage("Reference to property 'removedInlineVal' can not be evaluated: No property found for symbol '/removedInlineVal'")) { removedInlineValReferenceToString() }
    }

    // inline var
    expectSuccess(true) { createRemovedInlineVarReference() is kotlin.reflect.KMutableProperty0<*> }
    expectFailure(linkage("Reference to property 'removedInlineVar' can not be evaluated: No property found for symbol '/removedInlineVar'")) { removedInlineVarReferenceName() }
    expectSuccess(231) { removedInlineVarReferenceInvoke() }
    expectSuccess(231) { removedInlineVarReferenceGet() }
    expectSuccess(Unit) { removedInlineVarReferenceSet() }

    if (!testMode.isJs) {
        expectFailure(linkage("Reference to property 'removedInlineVar' can not be evaluated: No property found for symbol '/removedInlineVar'")) { removedInlineVarReferenceHashCode() }
        expectFailure(linkage("Reference to property 'removedInlineVar' can not be evaluated: No property found for symbol '/removedInlineVar'")) { removedInlineVarReferenceEquals() }
        expectFailure(linkage("Reference to property 'removedInlineVar' can not be evaluated: No property found for symbol '/removedInlineVar'")) { removedInlineVarReferenceToString() }
    }

    /*****************************************************/
    /***** Extracted from 'propertyTransformations': *****/
    /*****************************************************/

    val ocwpi: OpenClassWithPropertiesImpl = OpenClassWithPropertiesImpl()
    val ocwp: OpenClassWithProperties = ocwpi

    expectSuccess("OpenClassWithPropertiesV2.openNonInlineToInlineProperty") { openNonInlineToInlinePropertyInOpenClass(ocwp) }
    expectSuccess("OpenClassWithPropertiesV2.openNonInlineToInlinePropertyWithDelegation") { openNonInlineToInlinePropertyWithDelegationInOpenClass(ocwp) }
    expectSuccess("OpenClassWithPropertiesV2.newInlineProperty1") { newInlineProperty1InOpenClass(ocwp) }
    expectSuccess("OpenClassWithPropertiesV2.newInlineProperty2") { newInlineProperty2InOpenClass(ocwp) }
    expectSuccess( // TODO: this should be fixed in JS, KT-56762
        if (testMode.isJs) "OpenClassWithPropertiesImpl.newNonInlineProperty" else "OpenClassWithPropertiesV2.newNonInlineProperty"
    ) { newNonInlinePropertyInOpenClass(ocwp) }

    expectSuccess("OpenClassWithPropertiesImpl.openNonInlineToInlineProperty") { openNonInlineToInlinePropertyInOpenClassImpl(ocwpi) }
    expectSuccess("OpenClassWithPropertiesV2.openNonInlineToInlinePropertyWithDelegation called from OpenClassWithPropertiesImpl.openNonInlineToInlinePropertyWithDelegation") { openNonInlineToInlinePropertyWithDelegationInOpenClassImpl(ocwpi) }
    expectSuccess("OpenClassWithPropertiesImpl.newInlineProperty1") { newInlineProperty1InOpenClassImpl(ocwpi) }
    expectSuccess("OpenClassWithPropertiesImpl.newInlineProperty2") { newInlineProperty2InOpenClassImpl(ocwpi) }
    expectSuccess("OpenClassWithPropertiesImpl.newNonInlineProperty") { newNonInlinePropertyInOpenClassImpl(ocwpi) }

    expectSuccess("OpenClassWithPropertiesV2.openNonInlineToInlineProperty=a") { openNonInlineToInlinePropertyInOpenClass(ocwp, "a") }
    expectSuccess("OpenClassWithPropertiesV2.openNonInlineToInlinePropertyWithDelegation=b") { openNonInlineToInlinePropertyWithDelegationInOpenClass(ocwp, "b") }
    expectSuccess("OpenClassWithPropertiesV2.newInlineProperty1=c") { newInlineProperty1InOpenClass(ocwp, "c") }
    expectSuccess("OpenClassWithPropertiesV2.newInlineProperty2=d") { newInlineProperty2InOpenClass(ocwp, "d") }
    expectSuccess( // TODO: this should be fixed in JS, KT-56762
        if (testMode.isJs) "OpenClassWithPropertiesImpl.newNonInlineProperty=e" else "OpenClassWithPropertiesV2.newNonInlineProperty=e"
    ) { newNonInlinePropertyInOpenClass(ocwp, "e") }

    expectSuccess("OpenClassWithPropertiesImpl.openNonInlineToInlineProperty=f") { openNonInlineToInlinePropertyInOpenClassImpl(ocwpi, "f") }
    expectSuccess("OpenClassWithPropertiesV2.openNonInlineToInlinePropertyWithDelegation=h called from OpenClassWithPropertiesImpl.openNonInlineToInlinePropertyWithDelegation") { openNonInlineToInlinePropertyWithDelegationInOpenClassImpl(ocwpi, "h") }
    expectSuccess("OpenClassWithPropertiesImpl.newInlineProperty1=i") { newInlineProperty1InOpenClassImpl(ocwpi, "i") }
    expectSuccess("OpenClassWithPropertiesImpl.newInlineProperty2=j") { newInlineProperty2InOpenClassImpl(ocwpi, "j") }
    expectSuccess("OpenClassWithPropertiesImpl.newNonInlineProperty=k") { newNonInlinePropertyInOpenClassImpl(ocwpi, "k") }
}
