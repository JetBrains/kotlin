@file:Suppress("NOTHING_TO_INLINE")

import kotlin.coroutines.*

/**************************************************/
/***** Extracted from 'classTransformations': *****/
/**************************************************/

inline fun getClassToEnumFooInline(): ClassToEnum.Foo = ClassToEnum.Foo()
inline fun getClassToEnumFooAsAnyInline(): Any = ClassToEnum.Foo()

inline fun getClassToEnumBarInline(): ClassToEnum.Bar = ClassToEnum.Bar
inline fun getClassToEnumBarAsAnyInline(): Any = ClassToEnum.Bar

inline fun getClassToEnumBazInline(): ClassToEnum.Baz = ClassToEnum().Baz()
inline fun getClassToEnumBazAsAnyInline(): Any = ClassToEnum().Baz()

inline fun getObjectToEnumFooInline(): ObjectToEnum.Foo = ObjectToEnum.Foo()
inline fun getObjectToEnumFooAsAnyInline(): Any = ObjectToEnum.Foo()

inline fun getObjectToEnumBarInline(): ObjectToEnum.Bar = ObjectToEnum.Bar
inline fun getObjectToEnumBarAsAnyInline(): Any = ObjectToEnum.Bar

inline fun getEnumToClassFooInline(): EnumToClass = EnumToClass.Foo
inline fun getEnumToClassFooAsAnyInline(): Any = EnumToClass.Foo

inline fun getEnumToClassBarInline(): EnumToClass = EnumToClass.Bar
inline fun getEnumToClassBarAsAnyInline(): Any = EnumToClass.Bar

inline fun getEnumToClassBazInline(): EnumToClass = EnumToClass.Baz
inline fun getEnumToClassBazAsAnyInline(): Any = EnumToClass.Baz

inline fun getEnumToObjectFooInline(): EnumToObject = EnumToObject.Foo
inline fun getEnumToObjectFooAsAnyInline(): Any = EnumToObject.Foo

inline fun getEnumToObjectBarInline(): EnumToObject = EnumToObject.Bar
inline fun getEnumToObjectBarAsAnyInline(): Any = EnumToObject.Bar

inline fun getClassToObjectInline(): ClassToObject = ClassToObject()
inline fun getClassToObjectAsAnyInline(): Any = ClassToObject()

inline fun getObjectToClassInline(): ObjectToClass = ObjectToClass
inline fun getObjectToClassAsAnyInline(): Any = ObjectToClass

inline fun getClassToInterfaceInline(): ClassToInterface = ClassToInterface()
inline fun getClassToInterfaceAsAnyInline(): Any = ClassToInterface()

inline fun getNestedObjectToCompanion1Inline(): NestedObjectToCompanion1.Companion = NestedObjectToCompanion1.Companion
inline fun getNestedObjectToCompanion1AsAnyInline(): Any = NestedObjectToCompanion1.Companion

inline fun getNestedObjectToCompanion2Inline(): NestedObjectToCompanion2.Foo = NestedObjectToCompanion2.Foo
inline fun getNestedObjectToCompanion2AsAnyInline(): Any = NestedObjectToCompanion2.Foo

inline fun getCompanionToNestedObject1Inline(): CompanionToNestedObject1.Companion = CompanionToNestedObject1.Companion
inline fun getCompanionToNestedObject1AsAnyInline(): Any = CompanionToNestedObject1.Companion
inline fun getCompanionToNestedObject1NameInline(): String = CompanionToNestedObject1.Companion.name()
inline fun getCompanionToNestedObject1NameShortInline(): String = CompanionToNestedObject1.name() // "Companion" is omit

inline fun getCompanionToNestedObject2Inline(): CompanionToNestedObject2.Foo = CompanionToNestedObject2.Foo
inline fun getCompanionToNestedObject2AsAnyInline(): Any = CompanionToNestedObject2.Foo
inline fun getCompanionToNestedObject2NameInline(): String = CompanionToNestedObject2.Foo.name()
inline fun getCompanionToNestedObject2NameShortInline(): String = CompanionToNestedObject2.name() // "Foo" is omit

inline fun getCompanionAndNestedObjectsSwapInline(): String = CompanionAndNestedObjectsSwap.name() // companion object name is omit

inline fun getNestedToInnerNameInline() = NestedClassContainer.NestedToInner().name()
inline fun getNestedToInnerObjectNameInline() = NestedClassContainer.NestedToInner.Object.name()
inline fun getNestedToInnerCompanionNameInline() = NestedClassContainer.NestedToInner.name()
inline fun getNestedToInnerNestedNameInline() = NestedClassContainer.NestedToInner.Nested().name()
inline fun getNestedToInnerInnerNameInline() = NestedClassContainer.NestedToInner().Inner().name()

inline fun getInnerToNestedNameInline() = InnerClassContainer().InnerToNested().name()
inline fun getInnerToNestedObjectNameInline() = InnerClassContainer().InnerToNested().Object().name()
inline fun getInnerToNestedCompanionNameInline() = InnerClassContainer().InnerToNested().Companion().name()
inline fun getInnerToNestedNestedNameInline() = InnerClassContainer().InnerToNested().Nested().name()
inline fun getInnerToNestedInnerNameInline() = InnerClassContainer().InnerToNested().Inner().name()

annotation class AnnotationClassWithParameterThatBecomesRegularClass(val x: AnnotationClassThatBecomesRegularClass)
annotation class AnnotationClassWithParameterOfParameterThatBecomesRegularClass(val x: AnnotationClassWithParameterThatBecomesRegularClass)
annotation class AnnotationClassWithParameterThatDisappears(val x: AnnotationClassThatDisappears)
annotation class AnnotationClassWithParameterOfParameterThatDisappears(val x: AnnotationClassWithParameterThatDisappears)
annotation class AnnotationClassWithClassReferenceParameterThatDisappears1(val x: kotlin.reflect.KClass<RemovedClass> = RemovedClass::class)
annotation class AnnotationClassWithClassReferenceParameterThatDisappears2(val x: kotlin.reflect.KClass<*> = RemovedClass::class)
annotation class AnnotationClassWithClassReferenceParameterOfParameterThatDisappears1(val x: AnnotationClassWithClassReferenceParameterThatDisappears1 = AnnotationClassWithClassReferenceParameterThatDisappears1())
annotation class AnnotationClassWithClassReferenceParameterOfParameterThatDisappears2(val x: AnnotationClassWithClassReferenceParameterThatDisappears2 = AnnotationClassWithClassReferenceParameterThatDisappears2())
annotation class AnnotationClassWithRemovedEnumEntryParameter(val x: EnumClassWithDisappearingEntry = EnumClassWithDisappearingEntry.REMOVED)
annotation class AnnotationClassWithRemovedEnumEntryParameterOfParameter(val x: AnnotationClassWithRemovedEnumEntryParameter = AnnotationClassWithRemovedEnumEntryParameter())
annotation class AnnotationClassWithParameterThatBecomesPrivate1(val x: PublicTopLevelLib1.AnnotationClassThatBecomesPrivate = PublicTopLevelLib1.AnnotationClassThatBecomesPrivate())
annotation class AnnotationClassWithParameterThatBecomesPrivate2(val x: kotlin.reflect.KClass<PublicTopLevelLib1.ClassThatBecomesPrivate> = PublicTopLevelLib1.ClassThatBecomesPrivate::class)
annotation class AnnotationClassWithParameterOfParameterThatBecomesPrivate2(val x: AnnotationClassWithParameterThatBecomesPrivate2 = AnnotationClassWithParameterThatBecomesPrivate2())
annotation class AnnotationClassWithParameterThatBecomesPrivate3(val x: kotlin.reflect.KClass<*> = PublicTopLevelLib1.ClassThatBecomesPrivate::class)
annotation class AnnotationClassWithParameterOfParameterThatBecomesPrivate3(val x: AnnotationClassWithParameterThatBecomesPrivate3 = AnnotationClassWithParameterThatBecomesPrivate3())
annotation class AnnotationClassWithParameterThatBecomesPrivate4(val x: PublicTopLevelLib1.EnumClassThatBecomesPrivate = PublicTopLevelLib1.EnumClassThatBecomesPrivate.ENTRY)
annotation class AnnotationClassWithParameterOfParameterThatBecomesPrivate4(val x: AnnotationClassWithParameterThatBecomesPrivate4 = AnnotationClassWithParameterThatBecomesPrivate4())

object PublicTopLevelLib2 {
    private class PrivateClass
    annotation class AnnotationClassWithParameterWithPrivateDefaultValue(val x: kotlin.reflect.KClass<*> = PrivateClass::class)
    annotation class AnnotationClassWithParameterOfParameterWithPrivateDefaultValue(val x: AnnotationClassWithParameterWithPrivateDefaultValue = AnnotationClassWithParameterWithPrivateDefaultValue())
}

inline fun getAnnotationClassWithChangedParameterTypeInline(): AnnotationClassWithChangedParameterType = AnnotationClassWithChangedParameterType(102)
inline fun getAnnotationClassWithChangedParameterTypeAsAnyInline(): Any = AnnotationClassWithChangedParameterType(104)
inline fun getAnnotationClassThatBecomesRegularClassInline(): AnnotationClassThatBecomesRegularClass = AnnotationClassThatBecomesRegularClass(106)
inline fun getAnnotationClassThatBecomesRegularClassAsAnyInline(): Any = AnnotationClassThatBecomesRegularClass(108)
inline fun getAnnotationClassWithParameterThatBecomesRegularClassInline(): AnnotationClassWithParameterThatBecomesRegularClass = AnnotationClassWithParameterThatBecomesRegularClass(AnnotationClassThatBecomesRegularClass(110))
inline fun getAnnotationClassWithParameterThatBecomesRegularClassAsAnyInline(): Any = AnnotationClassWithParameterThatBecomesRegularClass(AnnotationClassThatBecomesRegularClass(112))
inline fun getAnnotationClassWithParameterOfParameterThatBecomesRegularClassInline(): AnnotationClassWithParameterOfParameterThatBecomesRegularClass = AnnotationClassWithParameterOfParameterThatBecomesRegularClass(AnnotationClassWithParameterThatBecomesRegularClass(AnnotationClassThatBecomesRegularClass(114)))
inline fun getAnnotationClassWithParameterOfParameterThatBecomesRegularClassAsAnyInline(): Any = AnnotationClassWithParameterOfParameterThatBecomesRegularClass(AnnotationClassWithParameterThatBecomesRegularClass(AnnotationClassThatBecomesRegularClass(116)))
inline fun getAnnotationClassThatDisappearsInline(): AnnotationClassThatDisappears = AnnotationClassThatDisappears(118)
inline fun getAnnotationClassThatDisappearsAsAnyInline(): Any = AnnotationClassThatDisappears(120)
inline fun getAnnotationClassWithParameterThatDisappearsInline(): AnnotationClassWithParameterThatDisappears = AnnotationClassWithParameterThatDisappears(AnnotationClassThatDisappears(122))
inline fun getAnnotationClassWithParameterThatDisappearsAsAnyInline(): Any = AnnotationClassWithParameterThatDisappears(AnnotationClassThatDisappears(124))
inline fun getAnnotationClassWithParameterOfParameterThatDisappearsInline(): AnnotationClassWithParameterOfParameterThatDisappears = AnnotationClassWithParameterOfParameterThatDisappears(AnnotationClassWithParameterThatDisappears(AnnotationClassThatDisappears(126)))
inline fun getAnnotationClassWithParameterOfParameterThatDisappearsAsAnyInline(): Any = AnnotationClassWithParameterOfParameterThatDisappears(AnnotationClassWithParameterThatDisappears(AnnotationClassThatDisappears(128)))
inline fun getAnnotationClassWithRenamedParametersInline(): AnnotationClassWithRenamedParameters = AnnotationClassWithRenamedParameters(130, "Pear")
inline fun getAnnotationClassWithRenamedParametersAsAnyInline(): Any = AnnotationClassWithRenamedParameters(132, "Peach")
inline fun getAnnotationClassWithReorderedParametersInline(): AnnotationClassWithReorderedParameters = AnnotationClassWithReorderedParameters(134, "Watermelon")
inline fun getAnnotationClassWithReorderedParametersAsAnyInline(): Any = AnnotationClassWithReorderedParameters(136, "Melon")
inline fun getAnnotationClassWithNewParameterInline(): AnnotationClassWithNewParameter = AnnotationClassWithNewParameter(138)
inline fun getAnnotationClassWithNewParameterAsAnyInline(): Any = AnnotationClassWithNewParameter(140)

inline fun getAnnotationClassWithClassReferenceParameterThatDisappears1Inline(): AnnotationClassWithClassReferenceParameterThatDisappears1 = AnnotationClassWithClassReferenceParameterThatDisappears1(RemovedClass::class)
inline fun getAnnotationClassWithClassReferenceParameterThatDisappears1AsAnyInline(): Any = AnnotationClassWithClassReferenceParameterThatDisappears1(RemovedClass::class)
inline fun getAnnotationClassWithDefaultClassReferenceParameterThatDisappears1Inline(): AnnotationClassWithClassReferenceParameterThatDisappears1 = AnnotationClassWithClassReferenceParameterThatDisappears1()
inline fun getAnnotationClassWithDefaultClassReferenceParameterThatDisappears1AsAnyInline(): Any = AnnotationClassWithClassReferenceParameterThatDisappears1()
inline fun getAnnotationClassWithClassReferenceParameterThatDisappears2Inline(): AnnotationClassWithClassReferenceParameterThatDisappears2 = AnnotationClassWithClassReferenceParameterThatDisappears2(RemovedClass::class)
inline fun getAnnotationClassWithClassReferenceParameterThatDisappears2AsAnyInline(): Any = AnnotationClassWithClassReferenceParameterThatDisappears2(RemovedClass::class)
inline fun getAnnotationClassWithDefaultClassReferenceParameterThatDisappears2Inline(): AnnotationClassWithClassReferenceParameterThatDisappears2 = AnnotationClassWithClassReferenceParameterThatDisappears2()
inline fun getAnnotationClassWithDefaultClassReferenceParameterThatDisappears2AsAnyInline(): Any = AnnotationClassWithClassReferenceParameterThatDisappears2()

inline fun getAnnotationClassWithClassReferenceParameterOfParameterThatDisappears1Inline(): AnnotationClassWithClassReferenceParameterOfParameterThatDisappears1 = AnnotationClassWithClassReferenceParameterOfParameterThatDisappears1(AnnotationClassWithClassReferenceParameterThatDisappears1(RemovedClass::class))
inline fun getAnnotationClassWithClassReferenceParameterOfParameterThatDisappears1AsAnyInline(): Any = AnnotationClassWithClassReferenceParameterOfParameterThatDisappears1(AnnotationClassWithClassReferenceParameterThatDisappears1(RemovedClass::class))
inline fun getAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears1Inline(): AnnotationClassWithClassReferenceParameterOfParameterThatDisappears1 = AnnotationClassWithClassReferenceParameterOfParameterThatDisappears1()
inline fun getAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears1AsAnyInline(): Any = AnnotationClassWithClassReferenceParameterOfParameterThatDisappears1()
inline fun getAnnotationClassWithClassReferenceParameterOfParameterThatDisappears2Inline(): AnnotationClassWithClassReferenceParameterOfParameterThatDisappears2 = AnnotationClassWithClassReferenceParameterOfParameterThatDisappears2(AnnotationClassWithClassReferenceParameterThatDisappears2(RemovedClass::class))
inline fun getAnnotationClassWithClassReferenceParameterOfParameterThatDisappears2AsAnyInline(): Any = AnnotationClassWithClassReferenceParameterOfParameterThatDisappears2(AnnotationClassWithClassReferenceParameterThatDisappears2(RemovedClass::class))
inline fun getAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears2Inline(): AnnotationClassWithClassReferenceParameterOfParameterThatDisappears2 = AnnotationClassWithClassReferenceParameterOfParameterThatDisappears2()
inline fun getAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears2AsAnyInline(): Any = AnnotationClassWithClassReferenceParameterOfParameterThatDisappears2()
inline fun getAnnotationClassWithRemovedEnumEntryParameterInline(): AnnotationClassWithRemovedEnumEntryParameter = AnnotationClassWithRemovedEnumEntryParameter(EnumClassWithDisappearingEntry.REMOVED)
inline fun getAnnotationClassWithRemovedEnumEntryParameterAsAnyInline(): Any = AnnotationClassWithRemovedEnumEntryParameter(EnumClassWithDisappearingEntry.REMOVED)
inline fun getAnnotationClassWithDefaultRemovedEnumEntryParameterInline(): AnnotationClassWithRemovedEnumEntryParameter = AnnotationClassWithRemovedEnumEntryParameter()
inline fun getAnnotationClassWithDefaultRemovedEnumEntryParameterAsAnyInline(): Any = AnnotationClassWithRemovedEnumEntryParameter()
inline fun getAnnotationClassWithRemovedEnumEntryParameterOfParameterInline(): AnnotationClassWithRemovedEnumEntryParameterOfParameter = AnnotationClassWithRemovedEnumEntryParameterOfParameter(AnnotationClassWithRemovedEnumEntryParameter(EnumClassWithDisappearingEntry.REMOVED))
inline fun getAnnotationClassWithRemovedEnumEntryParameterOfParameterAsAnyInline(): Any = AnnotationClassWithRemovedEnumEntryParameterOfParameter(AnnotationClassWithRemovedEnumEntryParameter(EnumClassWithDisappearingEntry.REMOVED))
inline fun getAnnotationClassWithDefaultRemovedEnumEntryParameterOfParameterInline(): AnnotationClassWithRemovedEnumEntryParameterOfParameter = AnnotationClassWithRemovedEnumEntryParameterOfParameter()
inline fun getAnnotationClassWithDefaultRemovedEnumEntryParameterOfParameterAsAnyInline(): Any = AnnotationClassWithRemovedEnumEntryParameterOfParameter()

inline fun getAnnotationClassWithParameterThatBecomesPrivate1Inline(): AnnotationClassWithParameterThatBecomesPrivate1 = AnnotationClassWithParameterThatBecomesPrivate1()
inline fun getAnnotationClassWithParameterThatBecomesPrivate1AsAnyInline(): Any = AnnotationClassWithParameterThatBecomesPrivate1()
inline fun getAnnotationClassWithParameterThatBecomesPrivate2Inline(): AnnotationClassWithParameterThatBecomesPrivate2 = AnnotationClassWithParameterThatBecomesPrivate2()
inline fun getAnnotationClassWithParameterThatBecomesPrivate2AsAnyInline(): Any = AnnotationClassWithParameterThatBecomesPrivate2()
inline fun getAnnotationClassWithParameterOfParameterThatBecomesPrivate2Inline(): AnnotationClassWithParameterOfParameterThatBecomesPrivate2 = AnnotationClassWithParameterOfParameterThatBecomesPrivate2()
inline fun getAnnotationClassWithParameterOfParameterThatBecomesPrivate2AsAnyInline(): Any = AnnotationClassWithParameterOfParameterThatBecomesPrivate2()
inline fun getAnnotationClassWithParameterThatBecomesPrivate3Inline(): AnnotationClassWithParameterThatBecomesPrivate3 = AnnotationClassWithParameterThatBecomesPrivate3()
inline fun getAnnotationClassWithParameterThatBecomesPrivate3AsAnyInline(): Any = AnnotationClassWithParameterThatBecomesPrivate3()
inline fun getAnnotationClassWithParameterOfParameterThatBecomesPrivate3Inline(): AnnotationClassWithParameterOfParameterThatBecomesPrivate3 = AnnotationClassWithParameterOfParameterThatBecomesPrivate3()
inline fun getAnnotationClassWithParameterOfParameterThatBecomesPrivate3AsAnyInline(): Any = AnnotationClassWithParameterOfParameterThatBecomesPrivate3()
inline fun getAnnotationClassWithParameterThatBecomesPrivate4Inline(): AnnotationClassWithParameterThatBecomesPrivate4 = AnnotationClassWithParameterThatBecomesPrivate4()
inline fun getAnnotationClassWithParameterThatBecomesPrivate4AsAnyInline(): Any = AnnotationClassWithParameterThatBecomesPrivate4()
inline fun getAnnotationClassWithParameterOfParameterThatBecomesPrivate4Inline(): AnnotationClassWithParameterOfParameterThatBecomesPrivate4 = AnnotationClassWithParameterOfParameterThatBecomesPrivate4()
inline fun getAnnotationClassWithParameterOfParameterThatBecomesPrivate4AsAnyInline(): Any = AnnotationClassWithParameterOfParameterThatBecomesPrivate4()
inline fun getAnnotationClassWithParameterWithPrivateDefaultValueInline(): PublicTopLevelLib2.AnnotationClassWithParameterWithPrivateDefaultValue = PublicTopLevelLib2.AnnotationClassWithParameterWithPrivateDefaultValue()
inline fun getAnnotationClassWithParameterWithPrivateDefaultValueInlineAsAny(): Any = PublicTopLevelLib2.AnnotationClassWithParameterWithPrivateDefaultValue()
inline fun getAnnotationClassWithParameterOfParameterWithPrivateDefaultValueInline(): PublicTopLevelLib2.AnnotationClassWithParameterOfParameterWithPrivateDefaultValue = PublicTopLevelLib2.AnnotationClassWithParameterOfParameterWithPrivateDefaultValue()
inline fun getAnnotationClassWithParameterOfParameterWithPrivateDefaultValueInlineAsAny(): Any = PublicTopLevelLib2.AnnotationClassWithParameterOfParameterWithPrivateDefaultValue()

@AnnotationClassWithChangedParameterType(1) class HolderOfAnnotationClassWithChangedParameterType { override fun toString() = "HolderOfAnnotationClassWithChangedParameterType" }
@AnnotationClassThatBecomesRegularClass(2) class HolderOfAnnotationClassThatBecomesRegularClass { override fun toString() = "HolderOfAnnotationClassThatBecomesRegularClass" }
@AnnotationClassWithParameterThatBecomesRegularClass(AnnotationClassThatBecomesRegularClass(3)) class HolderOfAnnotationClassWithParameterThatBecomesRegularClass { override fun toString() = "HolderOfAnnotationClassWithParameterThatBecomesRegularClass" }
@AnnotationClassWithParameterOfParameterThatBecomesRegularClass(AnnotationClassWithParameterThatBecomesRegularClass(AnnotationClassThatBecomesRegularClass(4))) class HolderOfAnnotationClassWithParameterOfParameterThatBecomesRegularClass { override fun toString() = "HolderOfAnnotationClassWithParameterOfParameterThatBecomesRegularClass" }
@AnnotationClassThatDisappears(5) class HolderOfAnnotationClassThatDisappears { override fun toString() = "HolderOfAnnotationClassThatDisappears" }
@AnnotationClassWithParameterThatDisappears(AnnotationClassThatDisappears(6)) class HolderOfAnnotationClassWithParameterThatDisappears { override fun toString() = "HolderOfAnnotationClassWithParameterThatDisappears" }
@AnnotationClassWithParameterOfParameterThatDisappears(AnnotationClassWithParameterThatDisappears(AnnotationClassThatDisappears(7))) class HolderOfAnnotationClassWithParameterOfParameterThatDisappears { override fun toString() = "HolderOfAnnotationClassWithParameterOfParameterThatDisappears" }
@AnnotationClassWithRenamedParameters(8, "Grape") class HolderOfAnnotationClassWithRenamedParameters { override fun toString() = "HolderOfAnnotationClassWithRenamedParameters" }
@AnnotationClassWithReorderedParameters(9, "Figs") class HolderOfAnnotationClassWithReorderedParameters { override fun toString() = "HolderOfAnnotationClassWithReorderedParameters" }
@AnnotationClassWithNewParameter(10) class HolderOfAnnotationClassWithNewParameter { override fun toString() = "HolderOfAnnotationClassWithNewParameter" }
@AnnotationClassWithClassReferenceParameterThatDisappears1(RemovedClass::class) class HolderOfAnnotationClassWithClassReferenceParameterThatDisappears1 { override fun toString() = "HolderOfAnnotationClassWithClassReferenceParameterThatDisappears1" }
@AnnotationClassWithClassReferenceParameterThatDisappears1() class HolderOfAnnotationClassWithDefaultClassReferenceParameterThatDisappears1 { override fun toString() = "HolderOfAnnotationClassWithDefaultClassReferenceParameterThatDisappears1" }
@AnnotationClassWithClassReferenceParameterThatDisappears2(RemovedClass::class) class HolderOfAnnotationClassWithClassReferenceParameterThatDisappears2 { override fun toString() = "HolderOfAnnotationClassWithClassReferenceParameterThatDisappears2" }
@AnnotationClassWithClassReferenceParameterThatDisappears2() class HolderOfAnnotationClassWithDefaultClassReferenceParameterThatDisappears2 { override fun toString() = "HolderOfAnnotationClassWithDefaultClassReferenceParameterThatDisappears2" }
@AnnotationClassWithClassReferenceParameterOfParameterThatDisappears1(AnnotationClassWithClassReferenceParameterThatDisappears1(RemovedClass::class)) class HolderOfAnnotationClassWithClassReferenceParameterOfParameterThatDisappears1 { override fun toString() = "HolderOfAnnotationClassWithClassReferenceParameterOfParameterThatDisappears1" }
@AnnotationClassWithClassReferenceParameterOfParameterThatDisappears1() class HolderOfAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears1 { override fun toString() = "HolderOfAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears1" }
@AnnotationClassWithClassReferenceParameterOfParameterThatDisappears2(AnnotationClassWithClassReferenceParameterThatDisappears2(RemovedClass::class)) class HolderOfAnnotationClassWithClassReferenceParameterOfParameterThatDisappears2 { override fun toString() = "HolderOfAnnotationClassWithClassReferenceParameterOfParameterThatDisappears2" }
@AnnotationClassWithClassReferenceParameterOfParameterThatDisappears2() class HolderOfAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears2 { override fun toString() = "HolderOfAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears2" }
@AnnotationClassWithRemovedEnumEntryParameter(EnumClassWithDisappearingEntry.REMOVED) class HolderOfAnnotationClassWithRemovedEnumEntryParameter { override fun toString() = "HolderOfAnnotationClassWithRemovedEnumEntryParameter" }
@AnnotationClassWithRemovedEnumEntryParameter() class HolderOfAnnotationClassWithDefaultRemovedEnumEntryParameter { override fun toString() = "HolderOfAnnotationClassWithDefaultRemovedEnumEntryParameter" }
@AnnotationClassWithRemovedEnumEntryParameterOfParameter(AnnotationClassWithRemovedEnumEntryParameter(EnumClassWithDisappearingEntry.REMOVED)) class HolderOfAnnotationClassWithRemovedEnumEntryParameterOfParameter { override fun toString() = "HolderOfAnnotationClassWithRemovedEnumEntryParameterOfParameter" }
@AnnotationClassWithRemovedEnumEntryParameterOfParameter() class HolderOfAnnotationClassWithDefaultRemovedEnumEntryParameterOfParameter { override fun toString() = "HolderOfAnnotationClassWithDefaultRemovedEnumEntryParameterOfParameter" }
@AnnotationClassWithParameterThatBecomesPrivate1 class HolderOfAnnotationClassWithParameterThatBecomesPrivate1 { override fun toString() = "HolderOfAnnotationClassWithParameterThatBecomesPrivate1" }
@AnnotationClassWithParameterThatBecomesPrivate2 class HolderOfAnnotationClassWithParameterThatBecomesPrivate2 { override fun toString() = "HolderOfAnnotationClassWithParameterThatBecomesPrivate2" }
@AnnotationClassWithParameterOfParameterThatBecomesPrivate2 class HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate2 { override fun toString() = "HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate2" }
@AnnotationClassWithParameterThatBecomesPrivate3 class HolderOfAnnotationClassWithParameterThatBecomesPrivate3 { override fun toString() = "HolderOfAnnotationClassWithParameterThatBecomesPrivate3" }
@AnnotationClassWithParameterOfParameterThatBecomesPrivate3 class HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate3 { override fun toString() = "HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate3" }
@AnnotationClassWithParameterThatBecomesPrivate4 class HolderOfAnnotationClassWithParameterThatBecomesPrivate4 { override fun toString() = "HolderOfAnnotationClassWithParameterThatBecomesPrivate4" }
@AnnotationClassWithParameterOfParameterThatBecomesPrivate4 class HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate4 { override fun toString() = "HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate4" }
@PublicTopLevelLib2.AnnotationClassWithParameterWithPrivateDefaultValue class HolderOfAnnotationClassWithParameterWithPrivateDefaultValue { override fun toString() = "HolderOfAnnotationClassWithParameterWithPrivateDefaultValue" }
@PublicTopLevelLib2.AnnotationClassWithParameterOfParameterWithPrivateDefaultValue class HolderOfAnnotationClassWithParameterOfParameterWithPrivateDefaultValue { override fun toString() = "HolderOfAnnotationClassWithParameterOfParameterWithPrivateDefaultValue" }

inline fun getHolderOfAnnotationClassWithChangedParameterTypeInline() = HolderOfAnnotationClassWithChangedParameterType()
inline fun getHolderOfAnnotationClassThatBecomesRegularClassInline() = HolderOfAnnotationClassThatBecomesRegularClass()
inline fun getHolderOfAnnotationClassWithParameterThatBecomesRegularClassInline() = HolderOfAnnotationClassWithParameterThatBecomesRegularClass()
inline fun getHolderOfAnnotationClassWithParameterOfParameterThatBecomesRegularClassInline() = HolderOfAnnotationClassWithParameterOfParameterThatBecomesRegularClass()
inline fun getHolderOfAnnotationClassThatDisappearsInline() = HolderOfAnnotationClassThatDisappears()
inline fun getHolderOfAnnotationClassWithParameterThatDisappearsInline() = HolderOfAnnotationClassWithParameterThatDisappears()
inline fun getHolderOfAnnotationClassWithParameterOfParameterThatDisappearsInline() = HolderOfAnnotationClassWithParameterOfParameterThatDisappears()
inline fun getHolderOfAnnotationClassWithRenamedParametersInline() = HolderOfAnnotationClassWithRenamedParameters()
inline fun getHolderOfAnnotationClassWithReorderedParametersInline() = HolderOfAnnotationClassWithReorderedParameters()
inline fun getHolderOfAnnotationClassWithNewParameterInline() = HolderOfAnnotationClassWithNewParameter()
inline fun getHolderOfAnnotationClassWithClassReferenceParameterThatDisappears1Inline() = HolderOfAnnotationClassWithClassReferenceParameterThatDisappears1()
inline fun getHolderOfAnnotationClassWithDefaultClassReferenceParameterThatDisappears1Inline() = HolderOfAnnotationClassWithDefaultClassReferenceParameterThatDisappears1()
inline fun getHolderOfAnnotationClassWithClassReferenceParameterThatDisappears2Inline() = HolderOfAnnotationClassWithClassReferenceParameterThatDisappears2()
inline fun getHolderOfAnnotationClassWithDefaultClassReferenceParameterThatDisappears2Inline() = HolderOfAnnotationClassWithDefaultClassReferenceParameterThatDisappears2()
inline fun getHolderOfAnnotationClassWithClassReferenceParameterOfParameterThatDisappears1Inline() = HolderOfAnnotationClassWithClassReferenceParameterOfParameterThatDisappears1()
inline fun getHolderOfAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears1Inline() = HolderOfAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears1()
inline fun getHolderOfAnnotationClassWithClassReferenceParameterOfParameterThatDisappears2Inline() = HolderOfAnnotationClassWithClassReferenceParameterOfParameterThatDisappears2()
inline fun getHolderOfAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears2Inline() = HolderOfAnnotationClassWithDefaultClassReferenceParameterOfParameterThatDisappears2()
inline fun getHolderOfAnnotationClassWithRemovedEnumEntryParameterInline() = HolderOfAnnotationClassWithRemovedEnumEntryParameter()
inline fun getHolderOfAnnotationClassWithDefaultRemovedEnumEntryParameterInline() = HolderOfAnnotationClassWithDefaultRemovedEnumEntryParameter()
inline fun getHolderOfAnnotationClassWithRemovedEnumEntryParameterOfParameterInline() = HolderOfAnnotationClassWithRemovedEnumEntryParameterOfParameter()
inline fun getHolderOfAnnotationClassWithDefaultRemovedEnumEntryParameterOfParameterInline() = HolderOfAnnotationClassWithDefaultRemovedEnumEntryParameterOfParameter()
inline fun getHolderOfAnnotationClassWithParameterThatBecomesPrivate1Inline(): HolderOfAnnotationClassWithParameterThatBecomesPrivate1 = HolderOfAnnotationClassWithParameterThatBecomesPrivate1()
inline fun getHolderOfAnnotationClassWithParameterThatBecomesPrivate2Inline(): HolderOfAnnotationClassWithParameterThatBecomesPrivate2 = HolderOfAnnotationClassWithParameterThatBecomesPrivate2()
inline fun getHolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate2Inline(): HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate2 = HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate2()
inline fun getHolderOfAnnotationClassWithParameterThatBecomesPrivate3Inline(): HolderOfAnnotationClassWithParameterThatBecomesPrivate3 = HolderOfAnnotationClassWithParameterThatBecomesPrivate3()
inline fun getHolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate3Inline(): HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate3 = HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate3()
inline fun getHolderOfAnnotationClassWithParameterThatBecomesPrivate4Inline(): HolderOfAnnotationClassWithParameterThatBecomesPrivate4 = HolderOfAnnotationClassWithParameterThatBecomesPrivate4()
inline fun getHolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate4Inline(): HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate4 = HolderOfAnnotationClassWithParameterOfParameterThatBecomesPrivate4()
inline fun getHolderOfAnnotationClassWithParameterWithPrivateDefaultValueInline(): HolderOfAnnotationClassWithParameterWithPrivateDefaultValue = HolderOfAnnotationClassWithParameterWithPrivateDefaultValue()
inline fun getHolderOfAnnotationClassWithParameterOfParameterWithPrivateDefaultValueInline(): HolderOfAnnotationClassWithParameterOfParameterWithPrivateDefaultValue = HolderOfAnnotationClassWithParameterOfParameterWithPrivateDefaultValue()

inline fun getValueToClassInline(): ValueToClass = ValueToClass(2)
inline fun getValueToClassAsAnyInline(): Any = ValueToClass(4)

inline fun getClassToValueInline(): ClassToValue = ClassToValue(2)
inline fun getClassToValueAsAnyInline(): Any = ClassToValue(4)

/*****************************************************/
/***** Extracted from 'functionTransformations': *****/
/*****************************************************/

// Auxiliary function to imitate coroutines.
private fun <R> runCoroutine(coroutine: suspend () -> R): R {
    var coroutineResult: Result<R>? = null

    coroutine.startCoroutine(Continuation(EmptyCoroutineContext) { result ->
        coroutineResult = result
    })

    return (coroutineResult ?: error("Coroutine finished without any result")).getOrThrow()
}

class OpenClassImpl : OpenClass() {
    override fun openNonInlineToInlineFunction(x: Int): String = "OpenClassImpl.openNonInlineToInlineFunction($x)"
    override fun openNonInlineToInlineFunctionWithDelegation(x: Int): String = super.openNonInlineToInlineFunctionWithDelegation(x) + " called from OpenClassImpl.openNonInlineToInlineFunctionWithDelegation($x)"
    fun newInlineFunction1(x: Int): String = "OpenClassImpl.newInlineFunction1($x)" // overrides accidentally appeared inline function
    @Suppress("NOTHING_TO_INLINE") inline fun newInlineFunction2(x: Int): String = "OpenClassImpl.newInlineFunction2($x)" // overrides accidentally appeared inline function
    @Suppress("NOTHING_TO_INLINE") inline fun newNonInlineFunction(x: Int): String = "OpenClassImpl.newNonInlineFunction($x)" // overrides accidentally appeared non-inline function
}

fun openNonInlineToInlineFunctionInOpenClass(oc: OpenClass, x: Int): String = oc.openNonInlineToInlineFunction(x)
fun openNonInlineToInlineFunctionWithDelegationInOpenClass(oc: OpenClass, x: Int): String = oc.openNonInlineToInlineFunctionWithDelegation(x)
fun newInlineFunction1InOpenClass(oc: OpenClass, x: Int): String = oc.newInlineFunction1Caller(x)
fun newInlineFunction2InOpenClass(oc: OpenClass, x: Int): String = oc.newInlineFunction2Caller(x)
fun newNonInlineFunctionInOpenClass(oc: OpenClass, x: Int): String = oc.newNonInlineFunctionCaller(x)
fun openNonInlineToInlineFunctionInOpenClassImpl(oci: OpenClassImpl, x: Int): String = oci.openNonInlineToInlineFunction(x)
fun openNonInlineToInlineFunctionWithDelegationInOpenClassImpl(oci: OpenClassImpl, x: Int): String = oci.openNonInlineToInlineFunctionWithDelegation(x)
fun newInlineFunction1InOpenClassImpl(oci: OpenClassImpl, x: Int): String = oci.newInlineFunction1(x)
fun newInlineFunction2InOpenClassImpl(oci: OpenClassImpl, x: Int): String = oci.newInlineFunction2(x)
fun newNonInlineFunctionInOpenClassImpl(oci: OpenClassImpl, x: Int): String = oci.newNonInlineFunction(x)

fun inlineLambdaToNoinlineLambda(x: Int): String = Functions.inlineLambdaToNoinlineLambda(x) { if (it > 0) it.toString() else return "inlineLambdaToNoinlineLambda($x)" }
fun inlineLambdaToCrossinlineLambda(x: Int): String = Functions.inlineLambdaToCrossinlineLambda(x) { if (it > 0) it.toString() else return "inlineLambdaToCrossinlineLambda($x)" }

private inline fun <R> runInlined(block: () -> R): R = block() // a-la kotlin.run() but without contracts and special annotation

fun suspendToNonSuspendFunction3(x: Int): Int = runCoroutine { runInlined { Functions.suspendToNonSuspendFunction(x) } }
fun nonSuspendToSuspendFunction3(x: Int): Int = runInlined { Functions.nonSuspendToSuspendFunction(x) }
fun nonSuspendToSuspendFunction4(x: Int): Int = runCoroutine { runInlined { Functions.nonSuspendToSuspendFunction(x) } }

/********************************************/
/***** Extracted from 'removeCallable': *****/
/********************************************/

inline fun callInlinedRemovedFunction() = removedFunction()
inline fun readInlinedRemovedProperty() = removedProperty

/*****************************************/
/***** Extracted from 'removeClass': *****/
/*****************************************/

open class OpenClassInheritedFromRemovedOpenClass : RemovedOpenClass()

inline fun inlinedFunctionWithRemovedOpenClassVariableType() {
    val foo: RemovedOpenClass? = null
    check(foo == null)
}

inline fun inlinedFunctionWithOpenClassImplVariableType() {
    val foo: OpenClassInheritedFromRemovedOpenClass? = null
    check(foo == null)
}

inline fun inlinedFunctionWithCreationOfRemovedOpenClass() {
    check(RemovedOpenClass().toString() != "Yellow Submarine")
}

inline fun inlinedFunctionWithCreationOfOpenClassImpl() {
    check(OpenClassInheritedFromRemovedOpenClass().toString() != "Yellow Submarine")
}

inline fun inlinedFunctionWithCreationOfRemovedOpenClassThroughReference() {
    check(run(::RemovedOpenClass).toString() != "Yellow Submarine")
}

inline fun inlinedFunctionWithCreationOfOpenClassImplThroughReference() {
    check(run(::OpenClassInheritedFromRemovedOpenClass).toString() != "Yellow Submarine")
}

inline fun inlinedFunctionWithRemovedOpenClassAnonymousObject() {
    val foo = object : RemovedOpenClass() {}
    check(foo.toString().isNotEmpty())
}

inline fun inlinedFunctionWithOpenClassImplAnonymousObject() {
    val foo = object : OpenClassInheritedFromRemovedOpenClass() {}
    check(foo.toString().isNotEmpty())
}

/***********************************************/
/***** Extracted from 'inheritanceIssues': *****/
/***********************************************/

inline fun getInterfaceToAbstractClassInline() = object : InterfaceToAbstractClass {}
inline fun getInterfaceToAbstractClassAsAnyInline(): Any = object : InterfaceToAbstractClass {}

inline fun getInterfaceToOpenClassInline() = object : InterfaceToOpenClass {}
inline fun getInterfaceToOpenClassAsAnyInline(): Any = object : InterfaceToOpenClass {}

inline fun getInterfaceToFinalClassInline() = object : InterfaceToFinalClass {}
inline fun getInterfaceToFinalClassAsAnyInline(): Any = object : InterfaceToFinalClass {}

open class InterfaceToAbstractClassImpl : InterfaceToAbstractClass
class InterfaceToAbstractClassImpl2 : InterfaceToAbstractClassImpl()
open class InterfaceToOpenClassImpl : InterfaceToOpenClass
class InterfaceToOpenClassImpl2 : InterfaceToOpenClassImpl()
open class InterfaceToFinalClassImpl : InterfaceToFinalClass
class InterfaceToFinalClassImpl2 : InterfaceToFinalClassImpl()

class InterfaceToAbstractClassContainer {
    open class InterfaceToAbstractClassImpl : InterfaceToAbstractClass
    class InterfaceToAbstractClassImpl2 : InterfaceToAbstractClassImpl()
    inner class InterfaceToAbstractClassInnerImpl : InterfaceToAbstractClass
}
class InterfaceToOpenClassContainer {
    open class InterfaceToOpenClassImpl : InterfaceToOpenClass
    class InterfaceToOpenClassImpl2 : InterfaceToOpenClassImpl()
    inner class InterfaceToOpenClassInnerImpl : InterfaceToOpenClass
}
class InterfaceToFinalClassContainer {
    open class InterfaceToFinalClassImpl : InterfaceToFinalClass
    class InterfaceToFinalClassImpl2 : InterfaceToFinalClassImpl()
    inner class InterfaceToFinalClassInnerImpl : InterfaceToFinalClass
}

inline fun getInterfaceToAbstractClassImplInline() = InterfaceToAbstractClassImpl()
inline fun getInterfaceToAbstractClassImplAsAnyInline(): Any = InterfaceToAbstractClassImpl()

inline fun getInterfaceToAbstractClassImpl2Inline() = InterfaceToAbstractClassImpl2()
inline fun getInterfaceToAbstractClassImpl2AsAnyInline(): Any = InterfaceToAbstractClassImpl2()

inline fun getInterfaceToOpenClassImplInline() = InterfaceToOpenClassImpl()
inline fun getInterfaceToOpenClassImplAsAnyInline(): Any = InterfaceToOpenClassImpl()

inline fun getInterfaceToOpenClassImpl2Inline() = InterfaceToOpenClassImpl2()
inline fun getInterfaceToOpenClassImpl2AsAnyInline(): Any = InterfaceToOpenClassImpl2()

inline fun getInterfaceToFinalClassImplInline() = InterfaceToFinalClassImpl()
inline fun getInterfaceToFinalClassImplAsAnyInline(): Any = InterfaceToFinalClassImpl()

inline fun getInterfaceToFinalClassImpl2Inline() = InterfaceToFinalClassImpl2()
inline fun getInterfaceToFinalClassImpl2AsAnyInline(): Any = InterfaceToFinalClassImpl2()

inline fun getInterfaceToAbstractClassNestedImplInline() = InterfaceToAbstractClassContainer.InterfaceToAbstractClassImpl()
inline fun getInterfaceToAbstractClassNestedImplAsAnyInline(): Any = InterfaceToAbstractClassContainer.InterfaceToAbstractClassImpl()

inline fun getInterfaceToAbstractClassNestedImpl2Inline() = InterfaceToAbstractClassContainer.InterfaceToAbstractClassImpl2()
inline fun getInterfaceToAbstractClassNestedImpl2AsAnyInline(): Any = InterfaceToAbstractClassContainer.InterfaceToAbstractClassImpl2()

inline fun getInterfaceToAbstractClassInnerImplInline() = InterfaceToAbstractClassContainer().InterfaceToAbstractClassInnerImpl()
inline fun getInterfaceToAbstractClassInnerImplAsAnyInline(): Any = InterfaceToAbstractClassContainer().InterfaceToAbstractClassInnerImpl()

inline fun getInterfaceToOpenClassNestedImplInline() = InterfaceToOpenClassContainer.InterfaceToOpenClassImpl()
inline fun getInterfaceToOpenClassNestedImplAsAnyInline(): Any = InterfaceToOpenClassContainer.InterfaceToOpenClassImpl()

inline fun getInterfaceToOpenClassNestedImpl2Inline() = InterfaceToOpenClassContainer.InterfaceToOpenClassImpl2()
inline fun getInterfaceToOpenClassNestedImpl2AsAnyInline(): Any = InterfaceToOpenClassContainer.InterfaceToOpenClassImpl2()

inline fun getInterfaceToOpenClassInnerImplInline() = InterfaceToOpenClassContainer().InterfaceToOpenClassInnerImpl()
inline fun getInterfaceToOpenClassInnerImplAsAnyInline(): Any = InterfaceToOpenClassContainer().InterfaceToOpenClassInnerImpl()

inline fun getInterfaceToFinalClassNestedImplInline() = InterfaceToFinalClassContainer.InterfaceToFinalClassImpl()
inline fun getInterfaceToFinalClassNestedImplAsAnyInline(): Any = InterfaceToFinalClassContainer.InterfaceToFinalClassImpl()

inline fun getInterfaceToFinalClassNestedImpl2Inline() = InterfaceToFinalClassContainer.InterfaceToFinalClassImpl2()
inline fun getInterfaceToFinalClassNestedImpl2AsAnyInline(): Any = InterfaceToFinalClassContainer.InterfaceToFinalClassImpl2()

inline fun getInterfaceToFinalClassInnerImplInline() = InterfaceToFinalClassContainer().InterfaceToFinalClassInnerImpl()
inline fun getInterfaceToFinalClassInnerImplAsAnyInline(): Any = InterfaceToFinalClassContainer().InterfaceToFinalClassInnerImpl()

inline fun referenceToInterfaceToAbstractClassImplInline() = InterfaceToAbstractClassImpl::class.simpleName.orEmpty()
inline fun referenceToInterfaceToAbstractClassImpl2Inline() = InterfaceToAbstractClassImpl2::class.simpleName.orEmpty()

inline fun referenceToInterfaceToFinalClassImplInline() = check(InterfaceToFinalClassImpl::class.simpleName != null)
inline fun referenceToInterfaceToFinalClassImpl2Inline() = check(InterfaceToFinalClassImpl2::class.simpleName != null)

inline fun getInterfaceToAbstractClass12_1Inline(): InterfaceToAbstractClass1 = object : InterfaceToAbstractClass1, InterfaceToAbstractClass2 {}
inline fun getInterfaceToAbstractClass12_2Inline(): InterfaceToAbstractClass2 = object : InterfaceToAbstractClass1, InterfaceToAbstractClass2 {}
inline fun getInterfaceToAbstractClass12AsAnyInline(): Any = object : InterfaceToAbstractClass1, InterfaceToAbstractClass2 {}

inline fun getInterfaceToAbstractClassAndAbstractClass_1Inline(): InterfaceToAbstractClass1 = object : InterfaceToAbstractClass1, AbstractClass() {}
inline fun getInterfaceToAbstractClassAndAbstractClass_2Inline(): AbstractClass = object : InterfaceToAbstractClass1, AbstractClass() {}
inline fun getInterfaceToAbstractClassAndAbstractClassAsAnyInline(): Any = object : InterfaceToAbstractClass1, AbstractClass() {}

open class InterfaceToAbstractClass12Impl : InterfaceToAbstractClass1, InterfaceToAbstractClass2
class InterfaceToAbstractClass12Impl2 : InterfaceToAbstractClass12Impl()
open class InterfaceToAbstractClassAndAbstractClassImpl : InterfaceToAbstractClass1, AbstractClass()
class InterfaceToAbstractClassAndAbstractClassImpl2 : InterfaceToAbstractClassAndAbstractClassImpl()

inline fun getInterfaceToAbstractClass12ImplInline() = InterfaceToAbstractClass12Impl()
inline fun getInterfaceToAbstractClass12ImplAsAnyInline(): Any = InterfaceToAbstractClass12Impl()

inline fun getInterfaceToAbstractClass12Impl2Inline() = InterfaceToAbstractClass12Impl2()
inline fun getInterfaceToAbstractClass12Impl2AsAnyInline(): Any = InterfaceToAbstractClass12Impl2()

inline fun getInterfaceToAbstractClassAndAbstractClassImplInline() = InterfaceToAbstractClassAndAbstractClassImpl()
inline fun getInterfaceToAbstractClassAndAbstractClassImplAsAnyInline(): Any = InterfaceToAbstractClassAndAbstractClassImpl()

inline fun getInterfaceToAbstractClassAndAbstractClassImpl2Inline() = InterfaceToAbstractClassAndAbstractClassImpl2()
inline fun getInterfaceToAbstractClassAndAbstractClassImpl2AsAnyInline(): Any = InterfaceToAbstractClassAndAbstractClassImpl2()

inline fun referenceToInterfaceToAbstractClass12ImplInline() = check(InterfaceToAbstractClass12Impl::class.simpleName != null)
inline fun referenceToInterfaceToAbstractClass12Impl2Inline() = check(InterfaceToAbstractClass12Impl2::class.simpleName != null)
inline fun referenceToInterfaceToAbstractClassAndAbstractClassImplInline() = check(InterfaceToAbstractClassAndAbstractClassImpl::class.simpleName != null)
inline fun referenceToInterfaceToAbstractClassAndAbstractClassImpl2Inline() = check(InterfaceToAbstractClassAndAbstractClassImpl2::class.simpleName != null)

/*************************************/
/***** Extracted from 'kt73511': *****/
/*************************************/

annotation class MyAnnotation

@MyAnnotationMarker(MyAnnotation::class)
data class MyAnnotationHolder(val x: Int)

/******************************************************/
/***** Extracted from 'richReferencesOperations': *****/
/******************************************************/

// inline fun
fun createRemovedInlineFunReference(): Any = ::removedInlineFun
fun removedInlineFunReferenceName(): String = ::removedInlineFun.name
fun removedInlineFunReferenceHashCode(): Int = ::removedInlineFun.hashCode()
fun removedInlineFunReferenceEquals(): Boolean = ::removedInlineFun.equals(Any())
fun removedInlineFunReferenceToString(): String = ::removedInlineFun.toString()
fun removedInlineFunReferenceInvoke(): Int = ::removedInlineFun.invoke(123)

// inline val
fun createRemovedInlineValReference(): Any = ::removedInlineVal
fun removedInlineValReferenceName(): String = ::removedInlineVal.name
fun removedInlineValReferenceHashCode(): Int = ::removedInlineVal.hashCode()
fun removedInlineValReferenceEquals(): Boolean = ::removedInlineVal.equals(Any())
fun removedInlineValReferenceToString(): String = ::removedInlineVal.toString()
fun removedInlineValReferenceInvoke(): Int = ::removedInlineVal.invoke()
fun removedInlineValReferenceGet(): Int = ::removedInlineVal.get()

// inline var
fun createRemovedInlineVarReference(): Any = ::removedInlineVar
fun removedInlineVarReferenceName(): String = ::removedInlineVar.name
fun removedInlineVarReferenceHashCode(): Int = ::removedInlineVar.hashCode()
fun removedInlineVarReferenceEquals(): Boolean = ::removedInlineVar.equals(Any())
fun removedInlineVarReferenceToString(): String = ::removedInlineVar.toString()
fun removedInlineVarReferenceInvoke(): Int = ::removedInlineVar.invoke()
fun removedInlineVarReferenceGet(): Int = ::removedInlineVar.get()
fun removedInlineVarReferenceSet(): Unit = ::removedInlineVar.set(123)

/*****************************************************/
/***** Extracted from 'propertyTransformations': *****/
/*****************************************************/

class OpenClassWithPropertiesImpl : OpenClassWithProperties() {
    override var openNonInlineToInlineProperty: String
        get() = "OpenClassWithPropertiesImpl.openNonInlineToInlineProperty"
        set(value) { lastRecordedState = "OpenClassWithPropertiesImpl.openNonInlineToInlineProperty=$value" }

    override var openNonInlineToInlinePropertyWithDelegation: String
        get() = super.openNonInlineToInlinePropertyWithDelegation + " called from OpenClassWithPropertiesImpl.openNonInlineToInlinePropertyWithDelegation"
        set(value) { super.openNonInlineToInlinePropertyWithDelegation = "$value called from OpenClassWithPropertiesImpl.openNonInlineToInlinePropertyWithDelegation" }

    var newInlineProperty1: String // overrides accidentally appeared inline property
        get() = "OpenClassWithPropertiesImpl.newInlineProperty1"
        set(value) { lastRecordedState = "OpenClassWithPropertiesImpl.newInlineProperty1=$value" }

    inline var newInlineProperty2: String // overrides accidentally appeared inline property
        get() = "OpenClassWithPropertiesImpl.newInlineProperty2"
        set(value) { lastRecordedState = "OpenClassWithPropertiesImpl.newInlineProperty2=$value" }

    inline var newNonInlineProperty: String // overrides accidentally appeared non-inline function
        get() = "OpenClassWithPropertiesImpl.newNonInlineProperty"
        set(value) { lastRecordedState = "OpenClassWithPropertiesImpl.newNonInlineProperty=$value" }
}

fun openNonInlineToInlinePropertyInOpenClass(oc: OpenClassWithProperties): String = oc.openNonInlineToInlineProperty
fun openNonInlineToInlinePropertyWithDelegationInOpenClass(oc: OpenClassWithProperties): String = oc.openNonInlineToInlinePropertyWithDelegation
fun newInlineProperty1InOpenClass(oc: OpenClassWithProperties): String = oc.newInlineProperty1Reader()
fun newInlineProperty2InOpenClass(oc: OpenClassWithProperties): String = oc.newInlineProperty2Reader()
fun newNonInlinePropertyInOpenClass(oc: OpenClassWithProperties): String = oc.newNonInlinePropertyReader()

fun openNonInlineToInlinePropertyInOpenClassImpl(oci: OpenClassWithPropertiesImpl): String = oci.openNonInlineToInlineProperty
fun openNonInlineToInlinePropertyWithDelegationInOpenClassImpl(oci: OpenClassWithPropertiesImpl): String = oci.openNonInlineToInlinePropertyWithDelegation
fun newInlineProperty1InOpenClassImpl(oci: OpenClassWithPropertiesImpl): String = oci.newInlineProperty1
fun newInlineProperty2InOpenClassImpl(oci: OpenClassWithPropertiesImpl): String = oci.newInlineProperty2
fun newNonInlinePropertyInOpenClassImpl(oci: OpenClassWithPropertiesImpl): String = oci.newNonInlineProperty

fun openNonInlineToInlinePropertyInOpenClass(oc: OpenClassWithProperties, value: String): String { oc.openNonInlineToInlineProperty = value; return oc.lastRecordedState }
fun openNonInlineToInlinePropertyWithDelegationInOpenClass(oc: OpenClassWithProperties, value: String): String { oc.openNonInlineToInlinePropertyWithDelegation = value; return oc.lastRecordedState }
fun newInlineProperty1InOpenClass(oc: OpenClassWithProperties, value: String): String { oc.newInlineProperty1Writer(value); return oc.lastRecordedState }
fun newInlineProperty2InOpenClass(oc: OpenClassWithProperties, value: String): String { oc.newInlineProperty2Writer(value); return oc.lastRecordedState }
fun newNonInlinePropertyInOpenClass(oc: OpenClassWithProperties, value: String): String { oc.newNonInlinePropertyWriter(value); return oc.lastRecordedState }

fun openNonInlineToInlinePropertyInOpenClassImpl(oci: OpenClassWithPropertiesImpl, value: String): String { oci.openNonInlineToInlineProperty = value; return oci.lastRecordedState }
fun openNonInlineToInlinePropertyWithDelegationInOpenClassImpl(oci: OpenClassWithPropertiesImpl, value: String): String { oci.openNonInlineToInlinePropertyWithDelegation = value; return oci.lastRecordedState }
fun newInlineProperty1InOpenClassImpl(oci: OpenClassWithPropertiesImpl, value: String): String { oci.newInlineProperty1 = value; return oci.lastRecordedState }
fun newInlineProperty2InOpenClassImpl(oci: OpenClassWithPropertiesImpl, value: String): String { oci.newInlineProperty2 = value; return oci.lastRecordedState }
fun newNonInlinePropertyInOpenClassImpl(oci: OpenClassWithPropertiesImpl, value: String): String { oci.newNonInlineProperty = value; return oci.lastRecordedState }
