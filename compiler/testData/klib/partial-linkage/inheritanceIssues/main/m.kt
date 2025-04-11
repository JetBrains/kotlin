import abitestutils.abiTest

fun box() = abiTest {
    val removedInterfaceImpl1 = RemovedInterfaceImpl1()
    val removedInterfaceImpl2 = RemovedInterfaceImpl2()
    val removedAbstractClassImpl1 = RemovedAbstractClassImpl1()
    val removedAbstractClassImpl2 = RemovedAbstractClassImpl2()
    val removedOpenClassImpl1 = RemovedOpenClassImpl1()
    val removedOpenClassImpl2 = RemovedOpenClassImpl2()
    val superSuperClassReplacedBySuperClass = SuperSuperClassReplacedBySuperClass()
    val superClassReplacedBySuperSuperClass = SuperClassReplacedBySuperSuperClass()

    expectFailure(linkage("Anonymous object initialization error: Constructor '<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClass() }
    expectFailure(linkage("Anonymous object initialization error: Constructor '<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassAsAny() }
    expectFailure(linkage("Class initialization error: Constructor 'Local.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassAsAny2() }

    expectFailure(linkage("Anonymous object initialization error: Constructor '<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClass() }
    expectFailure(linkage("Anonymous object initialization error: Constructor '<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassAsAny() }
    expectFailure(linkage("Class initialization error: Constructor 'Local.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassAsAny2() }

    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClass() }
    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassAsAny() }
    expectFailure(linkage("Constructor 'Local.<init>' can not be called: Class 'Local' inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassAsAny2() }

    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToAbstractClassImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassImpl() }
    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToAbstractClassImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassImplAsAny() }

    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToAbstractClassImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassImpl2() }
    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToAbstractClassImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassImpl2AsAny() }

    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToOpenClassImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassImpl() }
    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToOpenClassImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassImplAsAny() }

    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToOpenClassImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassImpl2() }
    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToOpenClassImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassImpl2AsAny() }

    expectFailure(linkage("Function 'getInterfaceToFinalClassImpl' can not be called: Function uses class 'InterfaceToFinalClassImpl' that inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassImpl() }
    expectFailure(linkage("Constructor 'InterfaceToFinalClassImpl.<init>' can not be called: Class 'InterfaceToFinalClassImpl' inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassImplAsAny() }

    expectFailure(linkage("Function 'getInterfaceToFinalClassImpl2' can not be called: Function uses class 'InterfaceToFinalClassImpl' (via class 'InterfaceToFinalClassImpl2') that inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassImpl2() }
    expectFailure(linkage("Constructor 'InterfaceToFinalClassImpl2.<init>' can not be called: Class 'InterfaceToFinalClassImpl2' uses class 'InterfaceToFinalClassImpl' that inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassImpl2AsAny() }

    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToAbstractClassImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassNestedImpl() }
    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToAbstractClassImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassNestedImplAsAny() }

    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToAbstractClassImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassNestedImpl2() }
    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToAbstractClassImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassNestedImpl2AsAny() }

    expectFailure(linkage("Inner class initialization error: Constructor 'InterfaceToAbstractClassInnerImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassInnerImpl() }
    expectFailure(linkage("Inner class initialization error: Constructor 'InterfaceToAbstractClassInnerImpl.<init>' should call a constructor of direct super class 'InterfaceToAbstractClass' but calls 'Any.<init>' instead")) { getInterfaceToAbstractClassInnerImplAsAny() }

    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToOpenClassImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassNestedImpl() }
    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToOpenClassImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassNestedImplAsAny() }

    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToOpenClassImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassNestedImpl2() }
    expectFailure(linkage("Class initialization error: Constructor 'InterfaceToOpenClassImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassNestedImpl2AsAny() }

    expectFailure(linkage("Inner class initialization error: Constructor 'InterfaceToOpenClassInnerImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassInnerImpl() }
    expectFailure(linkage("Inner class initialization error: Constructor 'InterfaceToOpenClassInnerImpl.<init>' should call a constructor of direct super class 'InterfaceToOpenClass' but calls 'Any.<init>' instead")) { getInterfaceToOpenClassInnerImplAsAny() }

    expectFailure(linkage("Function 'getInterfaceToFinalClassNestedImpl' can not be called: Function uses class 'InterfaceToFinalClassImpl' that inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassNestedImpl() }
    expectFailure(linkage("Constructor 'InterfaceToFinalClassImpl.<init>' can not be called: Class 'InterfaceToFinalClassImpl' inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassNestedImplAsAny() }

    expectFailure(linkage("Function 'getInterfaceToFinalClassNestedImpl2' can not be called: Function uses class 'InterfaceToFinalClassImpl' (via class 'InterfaceToFinalClassImpl2') that inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassNestedImpl2() }
    expectFailure(linkage("Constructor 'InterfaceToFinalClassImpl2.<init>' can not be called: Class 'InterfaceToFinalClassImpl2' uses class 'InterfaceToFinalClassImpl' that inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassNestedImpl2AsAny() }

    expectFailure(linkage("Function 'getInterfaceToFinalClassInnerImpl' can not be called: Function uses inner class 'InterfaceToFinalClassInnerImpl' that inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassInnerImpl() }
    expectFailure(linkage("Constructor 'InterfaceToFinalClassInnerImpl.<init>' can not be called: Inner class 'InterfaceToFinalClassInnerImpl' inherits from final class 'InterfaceToFinalClass'")) { getInterfaceToFinalClassInnerImplAsAny() }

    expectSuccess("InterfaceToAbstractClassImpl") { referenceToInterfaceToAbstractClassImpl() }
    expectSuccess("InterfaceToAbstractClassImpl2") { referenceToInterfaceToAbstractClassImpl2() }

    expectFailure(linkage("Reference to class 'InterfaceToFinalClassImpl' can not be evaluated: Expression uses class 'InterfaceToFinalClassImpl' that inherits from final class 'InterfaceToFinalClass'")) { referenceToInterfaceToFinalClassImpl() }
    expectFailure(linkage("Reference to class 'InterfaceToFinalClassImpl2' can not be evaluated: Expression uses class 'InterfaceToFinalClassImpl' (via class 'InterfaceToFinalClassImpl2') that inherits from final class 'InterfaceToFinalClass'")) { referenceToInterfaceToFinalClassImpl2() }

    expectFailure(linkage("Function 'getInterfaceToAnnotationClassImpl' can not be called: Function uses class 'InterfaceToAnnotationClassImpl' that has illegal inheritance from annotation class 'InterfaceToAnnotationClass'")) { getInterfaceToAnnotationClassImpl() }
    expectFailure(linkage("Constructor 'InterfaceToAnnotationClassImpl.<init>' can not be called: Class 'InterfaceToAnnotationClassImpl' has illegal inheritance from annotation class 'InterfaceToAnnotationClass'")) { getInterfaceToAnnotationClassImplAsAny() }
    expectFailure(linkage("Function 'getInterfaceToObjectImpl' can not be called: Function uses class 'InterfaceToObjectImpl' that inherits from final object 'InterfaceToObject'")) { getInterfaceToObjectImpl() }
    expectFailure(linkage("Constructor 'InterfaceToObjectImpl.<init>' can not be called: Class 'InterfaceToObjectImpl' inherits from final object 'InterfaceToObject'")) { getInterfaceToObjectImplAsAny() }
    expectFailure(linkage("Function 'getInterfaceToEnumClassImpl' can not be called: Function uses class 'InterfaceToEnumClassImpl' that inherits from final enum class 'InterfaceToEnumClass'")) { getInterfaceToEnumClassImpl() }
    expectFailure(linkage("Constructor 'InterfaceToEnumClassImpl.<init>' can not be called: Class 'InterfaceToEnumClassImpl' inherits from final enum class 'InterfaceToEnumClass'")) { getInterfaceToEnumClassImplAsAny() }
    expectFailure(linkage("Function 'getInterfaceToValueClassImpl' can not be called: Function uses class 'InterfaceToValueClassImpl' that inherits from final value class 'InterfaceToValueClass'")) { getInterfaceToValueClassImpl() }
    expectFailure(linkage("Constructor 'InterfaceToValueClassImpl.<init>' can not be called: Class 'InterfaceToValueClassImpl' inherits from final value class 'InterfaceToValueClass'")) { getInterfaceToValueClassImplAny() }
    expectFailure(linkage("Function 'getInterfaceToDataClassImpl' can not be called: Function uses class 'InterfaceToDataClassImpl' that inherits from final data class 'InterfaceToDataClass'")) { getInterfaceToDataClassImpl() }
    expectFailure(linkage("Constructor 'InterfaceToDataClassImpl.<init>' can not be called: Class 'InterfaceToDataClassImpl' inherits from final data class 'InterfaceToDataClass'")) { getInterfaceToDataClassImplAny() }

    expectFailure(linkage("Function 'getOpenClassToFinalClassImpl' can not be called: Function uses class 'OpenClassToFinalClassImpl' that inherits from final class 'OpenClassToFinalClass'")) { getOpenClassToFinalClassImpl() }
    expectFailure(linkage("Constructor 'OpenClassToFinalClassImpl.<init>' can not be called: Class 'OpenClassToFinalClassImpl' inherits from final class 'OpenClassToFinalClass'")) { getOpenClassToFinalClassImplAsAny() }
    expectFailure(linkage("Function 'getOpenClassToAnnotationClassImpl' can not be called: Function uses class 'OpenClassToAnnotationClassImpl' that has illegal inheritance from annotation class 'OpenClassToAnnotationClass'")) { getOpenClassToAnnotationClassImpl() }
    expectFailure(linkage("Constructor 'OpenClassToAnnotationClassImpl.<init>' can not be called: Class 'OpenClassToAnnotationClassImpl' has illegal inheritance from annotation class 'OpenClassToAnnotationClass'")) { getOpenClassToAnnotationClassImplAsAny() }
    expectFailure(linkage("Function 'getOpenClassToObjectImpl' can not be called: Function uses class 'OpenClassToObjectImpl' that inherits from final object 'OpenClassToObject'")) { getOpenClassToObjectImpl() }
    expectFailure(linkage("Constructor 'OpenClassToObjectImpl.<init>' can not be called: Class 'OpenClassToObjectImpl' inherits from final object 'OpenClassToObject'")) { getOpenClassToObjectImplAsAny() }
    expectFailure(linkage("Function 'getOpenClassToEnumClassImpl' can not be called: Function uses class 'OpenClassToEnumClassImpl' that inherits from final enum class 'OpenClassToEnumClass'")) { getOpenClassToEnumClassImpl() }
    expectFailure(linkage("Constructor 'OpenClassToEnumClassImpl.<init>' can not be called: Class 'OpenClassToEnumClassImpl' inherits from final enum class 'OpenClassToEnumClass'")) { getOpenClassToEnumClassImplAsAny() }
    expectFailure(linkage("Function 'getOpenClassToValueClassImpl' can not be called: Function uses class 'OpenClassToValueClassImpl' that inherits from final value class 'OpenClassToValueClass'")) { getOpenClassToValueClassImpl() }
    expectFailure(linkage("Constructor 'OpenClassToValueClassImpl.<init>' can not be called: Class 'OpenClassToValueClassImpl' inherits from final value class 'OpenClassToValueClass'")) { getOpenClassToValueClassImplAsAny() }
    expectFailure(linkage("Function 'getOpenClassToDataClassImpl' can not be called: Function uses class 'OpenClassToDataClassImpl' that inherits from final data class 'OpenClassToDataClass'")) { getOpenClassToDataClassImpl() }
    expectFailure(linkage("Constructor 'OpenClassToDataClassImpl.<init>' can not be called: Class 'OpenClassToDataClassImpl' inherits from final data class 'OpenClassToDataClass'")) { getOpenClassToDataClassImplAsAny() }
    expectFailure(linkage("Class initialization error: Constructor 'OpenClassToInterfaceImpl.<init>' should call a constructor of direct super class 'Any' but calls 'OpenClassToInterface.<init>' instead")) { getOpenClassToInterfaceImpl() }
    expectFailure(linkage("Class initialization error: Constructor 'OpenClassToInterfaceImpl.<init>' should call a constructor of direct super class 'Any' but calls 'OpenClassToInterface.<init>' instead")) { getOpenClassToInterfaceImplAsAny() }

    expectFailure(linkage("Function 'getValueClassInheritsAbstractClass' can not be called: Function uses value class 'ValueClassInheritsAbstractClass' that has illegal inheritance from class 'InterfaceToAbstractClass'")) { getValueClassInheritsAbstractClass() }
    expectFailure(linkage("Constructor 'ValueClassInheritsAbstractClass.<init>' can not be called: Value class 'ValueClassInheritsAbstractClass' has illegal inheritance from class 'InterfaceToAbstractClass'")) { getValueClassInheritsAbstractClassAsAny() }
    expectFailure(linkage("Function 'getEnumClassInheritsAbstractClass' can not be called: Function uses enum class 'EnumClassInheritsAbstractClass' that simultaneously inherits from 2 classes: 'Enum', 'InterfaceToAbstractClass'")) { getEnumClassInheritsAbstractClass() }
    expectFailure(linkage("Can not get instance of singleton 'EnumClassInheritsAbstractClass.ENTRY': Expression uses enum class 'EnumClassInheritsAbstractClass' that simultaneously inherits from 2 classes: 'Enum', 'InterfaceToAbstractClass'")) { getEnumClassInheritsAbstractClassAsAny() }

    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { getInterfaceToAbstractClass12_1() }
    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { getInterfaceToAbstractClass12_2() }
    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { getInterfaceToAbstractClass12AsAny() }

    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { getInterfaceToAbstractClassAndAbstractClass_1() }
    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { getInterfaceToAbstractClassAndAbstractClass_2() }
    expectFailure(linkage("Constructor '<init>' can not be called: Anonymous object simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { getInterfaceToAbstractClassAndAbstractClassAsAny() }

    expectFailure(linkage("Function 'getInterfaceToAbstractClass12Impl' can not be called: Function uses class 'InterfaceToAbstractClass12Impl' that simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { getInterfaceToAbstractClass12Impl() }
    expectFailure(linkage("Constructor 'InterfaceToAbstractClass12Impl.<init>' can not be called: Class 'InterfaceToAbstractClass12Impl' simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { getInterfaceToAbstractClass12ImplAsAny() }

    expectFailure(linkage("Function 'getInterfaceToAbstractClass12Impl2' can not be called: Function uses class 'InterfaceToAbstractClass12Impl' (via class 'InterfaceToAbstractClass12Impl2') that simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { getInterfaceToAbstractClass12Impl2() }
    expectFailure(linkage("Constructor 'InterfaceToAbstractClass12Impl2.<init>' can not be called: Class 'InterfaceToAbstractClass12Impl2' uses class 'InterfaceToAbstractClass12Impl' that simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { getInterfaceToAbstractClass12Impl2AsAny() }

    expectFailure(linkage("Function 'getInterfaceToAbstractClassAndAbstractClassImpl' can not be called: Function uses class 'InterfaceToAbstractClassAndAbstractClassImpl' that simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { getInterfaceToAbstractClassAndAbstractClassImpl() }
    expectFailure(linkage("Constructor 'InterfaceToAbstractClassAndAbstractClassImpl.<init>' can not be called: Class 'InterfaceToAbstractClassAndAbstractClassImpl' simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { getInterfaceToAbstractClassAndAbstractClassImplAsAny() }

    expectFailure(linkage("Function 'getInterfaceToAbstractClassAndAbstractClassImpl2' can not be called: Function uses class 'InterfaceToAbstractClassAndAbstractClassImpl' (via class 'InterfaceToAbstractClassAndAbstractClassImpl2') that simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { getInterfaceToAbstractClassAndAbstractClassImpl2() }
    expectFailure(linkage("Constructor 'InterfaceToAbstractClassAndAbstractClassImpl2.<init>' can not be called: Class 'InterfaceToAbstractClassAndAbstractClassImpl2' uses class 'InterfaceToAbstractClassAndAbstractClassImpl' that simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { getInterfaceToAbstractClassAndAbstractClassImpl2AsAny() }

    expectFailure(linkage("Reference to class 'InterfaceToAbstractClass12Impl' can not be evaluated: Expression uses class 'InterfaceToAbstractClass12Impl' that simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { referenceToInterfaceToAbstractClass12Impl() }
    expectFailure(linkage("Reference to class 'InterfaceToAbstractClass12Impl2' can not be evaluated: Expression uses class 'InterfaceToAbstractClass12Impl' (via class 'InterfaceToAbstractClass12Impl2') that simultaneously inherits from 2 classes: 'InterfaceToAbstractClass1', 'InterfaceToAbstractClass2'")) { referenceToInterfaceToAbstractClass12Impl2Impl() }
    expectFailure(linkage("Reference to class 'InterfaceToAbstractClassAndAbstractClassImpl' can not be evaluated: Expression uses class 'InterfaceToAbstractClassAndAbstractClassImpl' that simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { referenceToInterfaceToAbstractClassAndAbstractClassImpl() }
    expectFailure(linkage("Reference to class 'InterfaceToAbstractClassAndAbstractClassImpl2' can not be evaluated: Expression uses class 'InterfaceToAbstractClassAndAbstractClassImpl' (via class 'InterfaceToAbstractClassAndAbstractClassImpl2') that simultaneously inherits from 2 classes: 'AbstractClass', 'InterfaceToAbstractClass1'")) { referenceToInterfaceToAbstractClassAndAbstractClassImpl2Impl() }

    expectFailure(linkage("Can not read value from variable 'removedInterfaceImpl1': Variable uses unlinked class symbol '/RemovedInterface' (via class 'RemovedInterfaceImpl1')")) { removedInterfaceImpl1.abstractFun() }
    expectFailure(linkage("Can not read value from variable 'removedInterfaceImpl1': Variable uses unlinked class symbol '/RemovedInterface' (via class 'RemovedInterfaceImpl1')")) { removedInterfaceImpl1.abstractFunWithDefaultImpl() }
    expectFailure(linkage("Can not read value from variable 'removedInterfaceImpl1': Variable uses unlinked class symbol '/RemovedInterface' (via class 'RemovedInterfaceImpl1')")) { removedInterfaceImpl1.abstractVal }
    expectFailure(linkage("Can not read value from variable 'removedInterfaceImpl1': Variable uses unlinked class symbol '/RemovedInterface' (via class 'RemovedInterfaceImpl1')")) { removedInterfaceImpl1.abstractValWithDefaultImpl }

    expectFailure(linkage("Can not read value from variable 'removedInterfaceImpl2': Variable uses unlinked class symbol '/RemovedInterface' (via class 'RemovedInterfaceImpl2')")) { removedInterfaceImpl2.abstractFun() }
    expectFailure(linkage("Can not read value from variable 'removedInterfaceImpl2': Variable uses unlinked class symbol '/RemovedInterface' (via class 'RemovedInterfaceImpl2')")) { removedInterfaceImpl2.abstractFunWithDefaultImpl() }
    expectFailure(linkage("Can not read value from variable 'removedInterfaceImpl2': Variable uses unlinked class symbol '/RemovedInterface' (via class 'RemovedInterfaceImpl2')")) { removedInterfaceImpl2.abstractVal }
    expectFailure(linkage("Can not read value from variable 'removedInterfaceImpl2': Variable uses unlinked class symbol '/RemovedInterface' (via class 'RemovedInterfaceImpl2')")) { removedInterfaceImpl2.abstractValWithDefaultImpl }

    expectFailure(linkage("Can not read value from variable 'removedAbstractClassImpl1': Variable uses unlinked class symbol '/RemovedAbstractClass' (via class 'RemovedAbstractClassImpl1')")) { removedAbstractClassImpl1.abstractFun() }
    expectFailure(linkage("Can not read value from variable 'removedAbstractClassImpl1': Variable uses unlinked class symbol '/RemovedAbstractClass' (via class 'RemovedAbstractClassImpl1')")) { removedAbstractClassImpl1.openFun() }
    expectFailure(linkage("Can not read value from variable 'removedAbstractClassImpl1': Variable uses unlinked class symbol '/RemovedAbstractClass' (via class 'RemovedAbstractClassImpl1')")) { removedAbstractClassImpl1.finalFun() }
    expectFailure(linkage("Can not read value from variable 'removedAbstractClassImpl1': Variable uses unlinked class symbol '/RemovedAbstractClass' (via class 'RemovedAbstractClassImpl1')")) { removedAbstractClassImpl1.abstractVal }
    expectFailure(linkage("Can not read value from variable 'removedAbstractClassImpl1': Variable uses unlinked class symbol '/RemovedAbstractClass' (via class 'RemovedAbstractClassImpl1')")) { removedAbstractClassImpl1.openVal }
    expectFailure(linkage("Can not read value from variable 'removedAbstractClassImpl1': Variable uses unlinked class symbol '/RemovedAbstractClass' (via class 'RemovedAbstractClassImpl1')")) { removedAbstractClassImpl1.finalVal }

    expectFailure(linkage("Can not read value from variable 'removedAbstractClassImpl2': Variable uses unlinked class symbol '/RemovedAbstractClass' (via class 'RemovedAbstractClassImpl2')")) { removedAbstractClassImpl2.abstractFun() }
    expectFailure(linkage("Can not read value from variable 'removedAbstractClassImpl2': Variable uses unlinked class symbol '/RemovedAbstractClass' (via class 'RemovedAbstractClassImpl2')")) { removedAbstractClassImpl2.openFun() }
    expectFailure(linkage("Can not read value from variable 'removedAbstractClassImpl2': Variable uses unlinked class symbol '/RemovedAbstractClass' (via class 'RemovedAbstractClassImpl2')")) { removedAbstractClassImpl2.finalFun() }
    expectFailure(linkage("Can not read value from variable 'removedAbstractClassImpl2': Variable uses unlinked class symbol '/RemovedAbstractClass' (via class 'RemovedAbstractClassImpl2')")) { removedAbstractClassImpl2.abstractVal }
    expectFailure(linkage("Can not read value from variable 'removedAbstractClassImpl2': Variable uses unlinked class symbol '/RemovedAbstractClass' (via class 'RemovedAbstractClassImpl2')")) { removedAbstractClassImpl2.openVal }
    expectFailure(linkage("Can not read value from variable 'removedAbstractClassImpl2': Variable uses unlinked class symbol '/RemovedAbstractClass' (via class 'RemovedAbstractClassImpl2')")) { removedAbstractClassImpl2.finalVal }

    expectFailure(linkage("Can not read value from variable 'removedOpenClassImpl1': Variable uses unlinked class symbol '/RemovedOpenClass' (via class 'RemovedOpenClassImpl1')")) { removedOpenClassImpl1.openFun() }
    expectFailure(linkage("Can not read value from variable 'removedOpenClassImpl1': Variable uses unlinked class symbol '/RemovedOpenClass' (via class 'RemovedOpenClassImpl1')")) { removedOpenClassImpl1.finalFun() }
    expectFailure(linkage("Can not read value from variable 'removedOpenClassImpl1': Variable uses unlinked class symbol '/RemovedOpenClass' (via class 'RemovedOpenClassImpl1')")) { removedOpenClassImpl1.openVal }
    expectFailure(linkage("Can not read value from variable 'removedOpenClassImpl1': Variable uses unlinked class symbol '/RemovedOpenClass' (via class 'RemovedOpenClassImpl1')")) { removedOpenClassImpl1.finalVal }

    expectFailure(linkage("Can not read value from variable 'removedOpenClassImpl2': Variable uses unlinked class symbol '/RemovedOpenClass' (via class 'RemovedOpenClassImpl2')")) { removedOpenClassImpl2.openFun() }
    expectFailure(linkage("Can not read value from variable 'removedOpenClassImpl2': Variable uses unlinked class symbol '/RemovedOpenClass' (via class 'RemovedOpenClassImpl2')")) { removedOpenClassImpl2.finalFun() }
    expectFailure(linkage("Can not read value from variable 'removedOpenClassImpl2': Variable uses unlinked class symbol '/RemovedOpenClass' (via class 'RemovedOpenClassImpl2')")) { removedOpenClassImpl2.openVal }
    expectFailure(linkage("Can not read value from variable 'removedOpenClassImpl2': Variable uses unlinked class symbol '/RemovedOpenClass' (via class 'RemovedOpenClassImpl2')")) { removedOpenClassImpl2.finalVal }

    expectFailure(linkage("Constructor 'AbstractClassWithChangedConstructorSignature.<init>' can not be called: No constructor found for symbol '/AbstractClassWithChangedConstructorSignature.<init>'")) { AbstractClassWithChangedConstructorSignatureImpl() }
    expectFailure(linkage("Constructor 'OpenClassWithChangedConstructorSignature.<init>' can not be called: No constructor found for symbol '/OpenClassWithChangedConstructorSignature.<init>'")) { OpenClassWithChangedConstructorSignatureImpl() }

    expectSuccess("SuperSuperClassReplacedBySuperClass -> SuperClass -> SuperSuperClass -> Any") { superSuperClassReplacedBySuperClass.inheritsFrom() }
    expectSuccess(true) { SuperSuperClass::class.isInstance(superSuperClassReplacedBySuperClass) } // This check is done during the runtime.
    expectSuccess(true) { SuperClass::class.isInstance(superSuperClassReplacedBySuperClass) } // This check is done during the runtime.

    expectSuccess("SuperClassReplacedBySuperSuperClass -> SuperSuperClass -> Any") { superClassReplacedBySuperSuperClass.inheritsFrom() }
    expectSuccess(true) { SuperSuperClass::class.isInstance(superClassReplacedBySuperSuperClass) } // This check is done during the runtime.
    expectSuccess(false) { SuperClass::class.isInstance(superClassReplacedBySuperSuperClass) } // This check is done during the runtime.
}
