fun getClassToEnumFoo(): ClassToEnum.Foo = ClassToEnum.Foo()
inline fun getClassToEnumFooInline(): ClassToEnum.Foo = ClassToEnum.Foo()
fun getClassToEnumFooAsAny(): Any = ClassToEnum.Foo()
inline fun getClassToEnumFooAsAnyInline(): Any = ClassToEnum.Foo()

fun getClassToEnumBar(): ClassToEnum.Bar = ClassToEnum.Bar
inline fun getClassToEnumBarInline(): ClassToEnum.Bar = ClassToEnum.Bar
fun getClassToEnumBarAsAny(): Any = ClassToEnum.Bar
inline fun getClassToEnumBarAsAnyInline(): Any = ClassToEnum.Bar

fun getClassToEnumBaz(): ClassToEnum.Baz = ClassToEnum().Baz()
inline fun getClassToEnumBazInline(): ClassToEnum.Baz = ClassToEnum().Baz()
fun getClassToEnumBazAsAny(): Any = ClassToEnum().Baz()
inline fun getClassToEnumBazAsAnyInline(): Any = ClassToEnum().Baz()

fun getObjectToEnumFoo(): ObjectToEnum.Foo = ObjectToEnum.Foo()
inline fun getObjectToEnumFooInline(): ObjectToEnum.Foo = ObjectToEnum.Foo()
fun getObjectToEnumFooAsAny(): Any = ObjectToEnum.Foo()
inline fun getObjectToEnumFooAsAnyInline(): Any = ObjectToEnum.Foo()

fun getObjectToEnumBar(): ObjectToEnum.Bar = ObjectToEnum.Bar
inline fun getObjectToEnumBarInline(): ObjectToEnum.Bar = ObjectToEnum.Bar
fun getObjectToEnumBarAsAny(): Any = ObjectToEnum.Bar
inline fun getObjectToEnumBarAsAnyInline(): Any = ObjectToEnum.Bar

fun getEnumToClassFoo(): EnumToClass = EnumToClass.Foo
inline fun getEnumToClassFooInline(): EnumToClass = EnumToClass.Foo
fun getEnumToClassFooAsAny(): Any = EnumToClass.Foo
inline fun getEnumToClassFooAsAnyInline(): Any = EnumToClass.Foo

fun getEnumToClassBar(): EnumToClass = EnumToClass.Bar
inline fun getEnumToClassBarInline(): EnumToClass = EnumToClass.Bar
fun getEnumToClassBarAsAny(): Any = EnumToClass.Bar
inline fun getEnumToClassBarAsAnyInline(): Any = EnumToClass.Bar

fun getEnumToClassBaz(): EnumToClass = EnumToClass.Baz
inline fun getEnumToClassBazInline(): EnumToClass = EnumToClass.Baz
fun getEnumToClassBazAsAny(): Any = EnumToClass.Baz
inline fun getEnumToClassBazAsAnyInline(): Any = EnumToClass.Baz

fun getEnumToObjectFoo(): EnumToObject = EnumToObject.Foo
inline fun getEnumToObjectFooInline(): EnumToObject = EnumToObject.Foo
fun getEnumToObjectFooAsAny(): Any = EnumToObject.Foo
inline fun getEnumToObjectFooAsAnyInline(): Any = EnumToObject.Foo

fun getEnumToObjectBar(): EnumToObject = EnumToObject.Bar
inline fun getEnumToObjectBarInline(): EnumToObject = EnumToObject.Bar
fun getEnumToObjectBarAsAny(): Any = EnumToObject.Bar
inline fun getEnumToObjectBarAsAnyInline(): Any = EnumToObject.Bar

fun getClassToObject(): ClassToObject = ClassToObject()
inline fun getClassToObjectInline(): ClassToObject = ClassToObject()
fun getClassToObjectAsAny(): Any = ClassToObject()
inline fun getClassToObjectAsAnyInline(): Any = ClassToObject()

fun getObjectToClass(): ObjectToClass = ObjectToClass
inline fun getObjectToClassInline(): ObjectToClass = ObjectToClass
fun getObjectToClassAsAny(): Any = ObjectToClass
inline fun getObjectToClassAsAnyInline(): Any = ObjectToClass

fun getClassToInterface(): ClassToInterface = ClassToInterface()
inline fun getClassToInterfaceInline(): ClassToInterface = ClassToInterface()
fun getClassToInterfaceAsAny(): Any = ClassToInterface()
inline fun getClassToInterfaceAsAnyInline(): Any = ClassToInterface()

fun getNestedObjectToCompanion1(): NestedObjectToCompanion1.Companion = NestedObjectToCompanion1.Companion
inline fun getNestedObjectToCompanion1Inline(): NestedObjectToCompanion1.Companion = NestedObjectToCompanion1.Companion
fun getNestedObjectToCompanion1AsAny(): Any = NestedObjectToCompanion1.Companion
inline fun getNestedObjectToCompanion1AsAnyInline(): Any = NestedObjectToCompanion1.Companion

fun getNestedObjectToCompanion2(): NestedObjectToCompanion2.Foo = NestedObjectToCompanion2.Foo
inline fun getNestedObjectToCompanion2Inline(): NestedObjectToCompanion2.Foo = NestedObjectToCompanion2.Foo
fun getNestedObjectToCompanion2AsAny(): Any = NestedObjectToCompanion2.Foo
inline fun getNestedObjectToCompanion2AsAnyInline(): Any = NestedObjectToCompanion2.Foo

fun getCompanionToNestedObject1(): CompanionToNestedObject1.Companion = CompanionToNestedObject1.Companion
inline fun getCompanionToNestedObject1Inline(): CompanionToNestedObject1.Companion = CompanionToNestedObject1.Companion
fun getCompanionToNestedObject1AsAny(): Any = CompanionToNestedObject1.Companion
inline fun getCompanionToNestedObject1AsAnyInline(): Any = CompanionToNestedObject1.Companion
fun getCompanionToNestedObject1Name(): String = CompanionToNestedObject1.Companion.name()
inline fun getCompanionToNestedObject1NameInline(): String = CompanionToNestedObject1.Companion.name()
fun getCompanionToNestedObject1NameShort(): String = CompanionToNestedObject1.name() // "Companion" is omit
inline fun getCompanionToNestedObject1NameShortInline(): String = CompanionToNestedObject1.name() // "Companion" is omit

fun getCompanionToNestedObject2(): CompanionToNestedObject2.Foo = CompanionToNestedObject2.Foo
inline fun getCompanionToNestedObject2Inline(): CompanionToNestedObject2.Foo = CompanionToNestedObject2.Foo
fun getCompanionToNestedObject2AsAny(): Any = CompanionToNestedObject2.Foo
inline fun getCompanionToNestedObject2AsAnyInline(): Any = CompanionToNestedObject2.Foo
fun getCompanionToNestedObject2Name(): String = CompanionToNestedObject2.Foo.name()
inline fun getCompanionToNestedObject2NameInline(): String = CompanionToNestedObject2.Foo.name()
fun getCompanionToNestedObject2NameShort(): String = CompanionToNestedObject2.name() // "Foo" is omit
inline fun getCompanionToNestedObject2NameShortInline(): String = CompanionToNestedObject2.name() // "Foo" is omit

fun getCompanionAndNestedObjectsSwap(): String = CompanionAndNestedObjectsSwap.name() // companion object name is omit
inline fun getCompanionAndNestedObjectsSwapInline(): String = CompanionAndNestedObjectsSwap.name() // companion object name is omit

fun getNestedToInnerName() = NestedClassContainer.NestedToInner().name()
inline fun getNestedToInnerNameInline() = NestedClassContainer.NestedToInner().name()
fun getNestedToInnerObjectName() = NestedClassContainer.NestedToInner.Object.name()
inline fun getNestedToInnerObjectNameInline() = NestedClassContainer.NestedToInner.Object.name()
fun getNestedToInnerCompanionName() = NestedClassContainer.NestedToInner.name()
inline fun getNestedToInnerCompanionNameInline() = NestedClassContainer.NestedToInner.name()
fun getNestedToInnerNestedName() = NestedClassContainer.NestedToInner.Nested().name()
inline fun getNestedToInnerNestedNameInline() = NestedClassContainer.NestedToInner.Nested().name()
fun getNestedToInnerInnerName() = NestedClassContainer.NestedToInner().Inner().name()
inline fun getNestedToInnerInnerNameInline() = NestedClassContainer.NestedToInner().Inner().name()

fun getInnerToNestedName() = InnerClassContainer().InnerToNested().name()
inline fun getInnerToNestedNameInline() = InnerClassContainer().InnerToNested().name()
fun getInnerToNestedObjectName() = InnerClassContainer().InnerToNested().Object().name()
inline fun getInnerToNestedObjectNameInline() = InnerClassContainer().InnerToNested().Object().name()
fun getInnerToNestedCompanionName() = InnerClassContainer().InnerToNested().Companion().name()
inline fun getInnerToNestedCompanionNameInline() = InnerClassContainer().InnerToNested().Companion().name()
fun getInnerToNestedNestedName() = InnerClassContainer().InnerToNested().Nested().name()
inline fun getInnerToNestedNestedNameInline() = InnerClassContainer().InnerToNested().Nested().name()
fun getInnerToNestedInnerName() = InnerClassContainer().InnerToNested().Inner().name()
inline fun getInnerToNestedInnerNameInline() = InnerClassContainer().InnerToNested().Inner().name()

annotation class AnnotationClassWithParameterThatBecomesRegularClass(val x: AnnotationClassThatBecomesRegularClass)
annotation class AnnotationClassWithParameterOfParameterThatBecomesRegularClass(val x: AnnotationClassWithParameterThatBecomesRegularClass)
annotation class AnnotationClassWithParameterThatDisappears(val x: AnnotationClassThatDisappears)
annotation class AnnotationClassWithParameterOfParameterThatDisappears(val x: AnnotationClassWithParameterThatDisappears)

fun getAnnotationClassWithChangedParameterType(): AnnotationClassWithChangedParameterType = AnnotationClassWithChangedParameterType(101)
inline fun getAnnotationClassWithChangedParameterTypeInline(): AnnotationClassWithChangedParameterType = AnnotationClassWithChangedParameterType(102)
fun getAnnotationClassWithChangedParameterTypeAsAny(): Any = AnnotationClassWithChangedParameterType(103)
inline fun getAnnotationClassWithChangedParameterTypeAsAnyInline(): Any = AnnotationClassWithChangedParameterType(104)
fun getAnnotationClassThatBecomesRegularClass(): AnnotationClassThatBecomesRegularClass = AnnotationClassThatBecomesRegularClass(105)
inline fun getAnnotationClassThatBecomesRegularClassInline(): AnnotationClassThatBecomesRegularClass = AnnotationClassThatBecomesRegularClass(106)
fun getAnnotationClassThatBecomesRegularClassAsAny(): Any = AnnotationClassThatBecomesRegularClass(107)
inline fun getAnnotationClassThatBecomesRegularClassAsAnyInline(): Any = AnnotationClassThatBecomesRegularClass(108)
fun getAnnotationClassWithParameterThatBecomesRegularClass(): AnnotationClassWithParameterThatBecomesRegularClass = AnnotationClassWithParameterThatBecomesRegularClass(AnnotationClassThatBecomesRegularClass(109))
inline fun getAnnotationClassWithParameterThatBecomesRegularClassInline(): AnnotationClassWithParameterThatBecomesRegularClass = AnnotationClassWithParameterThatBecomesRegularClass(AnnotationClassThatBecomesRegularClass(110))
fun getAnnotationClassWithParameterThatBecomesRegularClassAsAny(): Any = AnnotationClassWithParameterThatBecomesRegularClass(AnnotationClassThatBecomesRegularClass(111))
inline fun getAnnotationClassWithParameterThatBecomesRegularClassAsAnyInline(): Any = AnnotationClassWithParameterThatBecomesRegularClass(AnnotationClassThatBecomesRegularClass(112))
fun getAnnotationClassWithParameterOfParameterThatBecomesRegularClass(): AnnotationClassWithParameterOfParameterThatBecomesRegularClass = AnnotationClassWithParameterOfParameterThatBecomesRegularClass(AnnotationClassWithParameterThatBecomesRegularClass(AnnotationClassThatBecomesRegularClass(113)))
inline fun getAnnotationClassWithParameterOfParameterThatBecomesRegularClassInline(): AnnotationClassWithParameterOfParameterThatBecomesRegularClass = AnnotationClassWithParameterOfParameterThatBecomesRegularClass(AnnotationClassWithParameterThatBecomesRegularClass(AnnotationClassThatBecomesRegularClass(114)))
fun getAnnotationClassWithParameterOfParameterThatBecomesRegularClassAsAny(): Any = AnnotationClassWithParameterOfParameterThatBecomesRegularClass(AnnotationClassWithParameterThatBecomesRegularClass(AnnotationClassThatBecomesRegularClass(115)))
inline fun getAnnotationClassWithParameterOfParameterThatBecomesRegularClassAsAnyInline(): Any = AnnotationClassWithParameterOfParameterThatBecomesRegularClass(AnnotationClassWithParameterThatBecomesRegularClass(AnnotationClassThatBecomesRegularClass(116)))
fun getAnnotationClassThatDisappears(): AnnotationClassThatDisappears = AnnotationClassThatDisappears(117)
inline fun getAnnotationClassThatDisappearsInline(): AnnotationClassThatDisappears = AnnotationClassThatDisappears(118)
fun getAnnotationClassThatDisappearsAsAny(): Any = AnnotationClassThatDisappears(119)
inline fun getAnnotationClassThatDisappearsAsAnyInline(): Any = AnnotationClassThatDisappears(120)
fun getAnnotationClassWithParameterThatDisappears(): AnnotationClassWithParameterThatDisappears = AnnotationClassWithParameterThatDisappears(AnnotationClassThatDisappears(121))
inline fun getAnnotationClassWithParameterThatDisappearsInline(): AnnotationClassWithParameterThatDisappears = AnnotationClassWithParameterThatDisappears(AnnotationClassThatDisappears(122))
fun getAnnotationClassWithParameterThatDisappearsAsAny(): Any = AnnotationClassWithParameterThatDisappears(AnnotationClassThatDisappears(123))
inline fun getAnnotationClassWithParameterThatDisappearsAsAnyInline(): Any = AnnotationClassWithParameterThatDisappears(AnnotationClassThatDisappears(124))
fun getAnnotationClassWithParameterOfParameterThatDisappears(): AnnotationClassWithParameterOfParameterThatDisappears = AnnotationClassWithParameterOfParameterThatDisappears(AnnotationClassWithParameterThatDisappears(AnnotationClassThatDisappears(125)))
inline fun getAnnotationClassWithParameterOfParameterThatDisappearsInline(): AnnotationClassWithParameterOfParameterThatDisappears = AnnotationClassWithParameterOfParameterThatDisappears(AnnotationClassWithParameterThatDisappears(AnnotationClassThatDisappears(126)))
fun getAnnotationClassWithParameterOfParameterThatDisappearsAsAny(): Any = AnnotationClassWithParameterOfParameterThatDisappears(AnnotationClassWithParameterThatDisappears(AnnotationClassThatDisappears(127)))
inline fun getAnnotationClassWithParameterOfParameterThatDisappearsAsAnyInline(): Any = AnnotationClassWithParameterOfParameterThatDisappears(AnnotationClassWithParameterThatDisappears(AnnotationClassThatDisappears(128)))
fun getAnnotationClassWithRenamedParameters(): AnnotationClassWithRenamedParameters = AnnotationClassWithRenamedParameters(129, "Banana")
inline fun getAnnotationClassWithRenamedParametersInline(): AnnotationClassWithRenamedParameters = AnnotationClassWithRenamedParameters(130, "Pear")
fun getAnnotationClassWithRenamedParametersAsAny(): Any = AnnotationClassWithRenamedParameters(131, "Orange")
inline fun getAnnotationClassWithRenamedParametersAsAnyInline(): Any = AnnotationClassWithRenamedParameters(132, "Peach")
fun getAnnotationClassWithReorderedParameters(): AnnotationClassWithReorderedParameters = AnnotationClassWithReorderedParameters(133, "Kiwi")
inline fun getAnnotationClassWithReorderedParametersInline(): AnnotationClassWithReorderedParameters = AnnotationClassWithReorderedParameters(134, "Watermelon")
fun getAnnotationClassWithReorderedParametersAsAny(): Any = AnnotationClassWithReorderedParameters(135, "Grapefruit")
inline fun getAnnotationClassWithReorderedParametersAsAnyInline(): Any = AnnotationClassWithReorderedParameters(136, "Melon")
fun getAnnotationClassWithNewParameter(): AnnotationClassWithNewParameter = AnnotationClassWithNewParameter(137)
inline fun getAnnotationClassWithNewParameterInline(): AnnotationClassWithNewParameter = AnnotationClassWithNewParameter(138)
fun getAnnotationClassWithNewParameterAsAny(): Any = AnnotationClassWithNewParameter(139)
inline fun getAnnotationClassWithNewParameterAsAnyInline(): Any = AnnotationClassWithNewParameter(140)

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

fun getHolderOfAnnotationClassWithChangedParameterType() = HolderOfAnnotationClassWithChangedParameterType()
inline fun getHolderOfAnnotationClassWithChangedParameterTypeInline() = HolderOfAnnotationClassWithChangedParameterType()
fun getHolderOfAnnotationClassThatBecomesRegularClass() = HolderOfAnnotationClassThatBecomesRegularClass()
inline fun getHolderOfAnnotationClassThatBecomesRegularClassInline() = HolderOfAnnotationClassThatBecomesRegularClass()
fun getHolderOfAnnotationClassWithParameterThatBecomesRegularClass() = HolderOfAnnotationClassWithParameterThatBecomesRegularClass()
inline fun getHolderOfAnnotationClassWithParameterThatBecomesRegularClassInline() = HolderOfAnnotationClassWithParameterThatBecomesRegularClass()
fun getHolderOfAnnotationClassWithParameterOfParameterThatBecomesRegularClass() = HolderOfAnnotationClassWithParameterOfParameterThatBecomesRegularClass()
inline fun getHolderOfAnnotationClassWithParameterOfParameterThatBecomesRegularClassInline() = HolderOfAnnotationClassWithParameterOfParameterThatBecomesRegularClass()
fun getHolderOfAnnotationClassThatDisappears() = HolderOfAnnotationClassThatDisappears()
inline fun getHolderOfAnnotationClassThatDisappearsInline() = HolderOfAnnotationClassThatDisappears()
fun getHolderOfAnnotationClassWithParameterThatDisappears() = HolderOfAnnotationClassWithParameterThatDisappears()
inline fun getHolderOfAnnotationClassWithParameterThatDisappearsInline() = HolderOfAnnotationClassWithParameterThatDisappears()
fun getHolderOfAnnotationClassWithParameterOfParameterThatDisappears() = HolderOfAnnotationClassWithParameterOfParameterThatDisappears()
inline fun getHolderOfAnnotationClassWithParameterOfParameterThatDisappearsInline() = HolderOfAnnotationClassWithParameterOfParameterThatDisappears()
fun getHolderOfAnnotationClassWithRenamedParameters() = HolderOfAnnotationClassWithRenamedParameters()
inline fun getHolderOfAnnotationClassWithRenamedParametersInline() = HolderOfAnnotationClassWithRenamedParameters()
fun getHolderOfAnnotationClassWithReorderedParameters() = HolderOfAnnotationClassWithReorderedParameters()
inline fun getHolderOfAnnotationClassWithReorderedParametersInline() = HolderOfAnnotationClassWithReorderedParameters()
fun getHolderOfAnnotationClassWithNewParameter() = HolderOfAnnotationClassWithNewParameter()
inline fun getHolderOfAnnotationClassWithNewParameterInline() = HolderOfAnnotationClassWithNewParameter()

fun getValueToClass(): ValueToClass = ValueToClass(1)
inline fun getValueToClassInline(): ValueToClass = ValueToClass(2)
fun getValueToClassAsAny(): Any = ValueToClass(3)
inline fun getValueToClassAsAnyInline(): Any = ValueToClass(4)

fun getClassToValue(): ClassToValue = ClassToValue(1)
inline fun getClassToValueInline(): ClassToValue = ClassToValue(2)
fun getClassToValueAsAny(): Any = ClassToValue(3)
inline fun getClassToValueAsAnyInline(): Any = ClassToValue(4)

fun getSumFromDataClass(): Int {
    val (x, y) = DataToClass(1, 2)
    return x + y
}

fun getFunctionalInterfaceToInterface(): FunctionalInterfaceToInterface {
    val worker = FunctionalInterfaceToInterface { /* do some work */ }
    worker.work()
    return worker
}
