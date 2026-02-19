fun getInterfaceToAbstractClass() = object : InterfaceToAbstractClass {}
fun getInterfaceToAbstractClassAsAny(): Any = object : InterfaceToAbstractClass {}
fun getInterfaceToAbstractClassAsAny2(): Any { class Local : InterfaceToAbstractClass; return Local() }

fun getInterfaceToOpenClass() = object : InterfaceToOpenClass {}
fun getInterfaceToOpenClassAsAny(): Any = object : InterfaceToOpenClass {}
fun getInterfaceToOpenClassAsAny2(): Any { class Local : InterfaceToOpenClass; return Local() }

fun getInterfaceToFinalClass() = object : InterfaceToFinalClass {}
fun getInterfaceToFinalClassAsAny(): Any = object : InterfaceToFinalClass {}
fun getInterfaceToFinalClassAsAny2(): Any { class Local : InterfaceToFinalClass; return Local() }

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

fun getInterfaceToAbstractClassImpl() = InterfaceToAbstractClassImpl()
fun getInterfaceToAbstractClassImplAsAny(): Any = InterfaceToAbstractClassImpl()

fun getInterfaceToAbstractClassImpl2() = InterfaceToAbstractClassImpl2()
fun getInterfaceToAbstractClassImpl2AsAny(): Any = InterfaceToAbstractClassImpl2()

fun getInterfaceToOpenClassImpl() = InterfaceToOpenClassImpl()
fun getInterfaceToOpenClassImplAsAny(): Any = InterfaceToOpenClassImpl()

fun getInterfaceToOpenClassImpl2() = InterfaceToOpenClassImpl2()
fun getInterfaceToOpenClassImpl2AsAny(): Any = InterfaceToOpenClassImpl2()

fun getInterfaceToFinalClassImpl() = InterfaceToFinalClassImpl()
fun getInterfaceToFinalClassImplAsAny(): Any = InterfaceToFinalClassImpl()

fun getInterfaceToFinalClassImpl2() = InterfaceToFinalClassImpl2()
fun getInterfaceToFinalClassImpl2AsAny(): Any = InterfaceToFinalClassImpl2()

fun getInterfaceToAbstractClassNestedImpl() = InterfaceToAbstractClassContainer.InterfaceToAbstractClassImpl()
fun getInterfaceToAbstractClassNestedImplAsAny(): Any = InterfaceToAbstractClassContainer.InterfaceToAbstractClassImpl()

fun getInterfaceToAbstractClassNestedImpl2() = InterfaceToAbstractClassContainer.InterfaceToAbstractClassImpl2()
fun getInterfaceToAbstractClassNestedImpl2AsAny(): Any = InterfaceToAbstractClassContainer.InterfaceToAbstractClassImpl2()

fun getInterfaceToAbstractClassInnerImpl() = InterfaceToAbstractClassContainer().InterfaceToAbstractClassInnerImpl()
fun getInterfaceToAbstractClassInnerImplAsAny(): Any = InterfaceToAbstractClassContainer().InterfaceToAbstractClassInnerImpl()

fun getInterfaceToOpenClassNestedImpl() = InterfaceToOpenClassContainer.InterfaceToOpenClassImpl()
fun getInterfaceToOpenClassNestedImplAsAny(): Any = InterfaceToOpenClassContainer.InterfaceToOpenClassImpl()

fun getInterfaceToOpenClassNestedImpl2() = InterfaceToOpenClassContainer.InterfaceToOpenClassImpl2()
fun getInterfaceToOpenClassNestedImpl2AsAny(): Any = InterfaceToOpenClassContainer.InterfaceToOpenClassImpl2()

fun getInterfaceToOpenClassInnerImpl() = InterfaceToOpenClassContainer().InterfaceToOpenClassInnerImpl()
fun getInterfaceToOpenClassInnerImplAsAny(): Any = InterfaceToOpenClassContainer().InterfaceToOpenClassInnerImpl()

fun getInterfaceToFinalClassNestedImpl() = InterfaceToFinalClassContainer.InterfaceToFinalClassImpl()
fun getInterfaceToFinalClassNestedImplAsAny(): Any = InterfaceToFinalClassContainer.InterfaceToFinalClassImpl()

fun getInterfaceToFinalClassNestedImpl2() = InterfaceToFinalClassContainer.InterfaceToFinalClassImpl2()
fun getInterfaceToFinalClassNestedImpl2AsAny(): Any = InterfaceToFinalClassContainer.InterfaceToFinalClassImpl2()

fun getInterfaceToFinalClassInnerImpl() = InterfaceToFinalClassContainer().InterfaceToFinalClassInnerImpl()
fun getInterfaceToFinalClassInnerImplAsAny(): Any = InterfaceToFinalClassContainer().InterfaceToFinalClassInnerImpl()

fun referenceToInterfaceToAbstractClassImpl() = InterfaceToAbstractClassImpl::class.simpleName.orEmpty()
fun referenceToInterfaceToAbstractClassImpl2() = InterfaceToAbstractClassImpl2::class.simpleName.orEmpty()

fun referenceToInterfaceToFinalClassImpl() = check(InterfaceToFinalClassImpl::class.simpleName != null)
fun referenceToInterfaceToFinalClassImpl2() = check(InterfaceToFinalClassImpl2::class.simpleName != null)

class InterfaceToAnnotationClassImpl : InterfaceToAnnotationClass
class InterfaceToObjectImpl : InterfaceToObject
class InterfaceToEnumClassImpl : InterfaceToEnumClass
class InterfaceToValueClassImpl : InterfaceToValueClass
class InterfaceToDataClassImpl : InterfaceToDataClass

fun getInterfaceToAnnotationClassImpl() = InterfaceToAnnotationClassImpl()
fun getInterfaceToAnnotationClassImplAsAny(): Any = InterfaceToAnnotationClassImpl()
fun getInterfaceToObjectImpl() = InterfaceToObjectImpl()
fun getInterfaceToObjectImplAsAny(): Any = InterfaceToObjectImpl()
fun getInterfaceToEnumClassImpl() = InterfaceToEnumClassImpl()
fun getInterfaceToEnumClassImplAsAny(): Any = InterfaceToEnumClassImpl()
fun getInterfaceToValueClassImpl() = InterfaceToValueClassImpl()
fun getInterfaceToValueClassImplAny(): Any = InterfaceToValueClassImpl()
fun getInterfaceToDataClassImpl() = InterfaceToDataClassImpl()
fun getInterfaceToDataClassImplAny(): Any = InterfaceToDataClassImpl()

class OpenClassToFinalClassImpl : OpenClassToFinalClass(42)
class OpenClassToAnnotationClassImpl : OpenClassToAnnotationClass(42)
class OpenClassToObjectImpl : OpenClassToObject(42)
class OpenClassToEnumClassImpl : OpenClassToEnumClass(42)
class OpenClassToValueClassImpl : OpenClassToValueClass(42)
class OpenClassToDataClassImpl : OpenClassToDataClass(42)
class OpenClassToInterfaceImpl : OpenClassToInterface(42)

fun getOpenClassToFinalClassImpl() = OpenClassToFinalClassImpl()
fun getOpenClassToFinalClassImplAsAny(): Any = OpenClassToFinalClassImpl()
fun getOpenClassToAnnotationClassImpl() = OpenClassToAnnotationClassImpl()
fun getOpenClassToAnnotationClassImplAsAny(): Any = OpenClassToAnnotationClassImpl()
fun getOpenClassToObjectImpl() = OpenClassToObjectImpl()
fun getOpenClassToObjectImplAsAny(): Any = OpenClassToObjectImpl()
fun getOpenClassToEnumClassImpl() = OpenClassToEnumClassImpl()
fun getOpenClassToEnumClassImplAsAny(): Any = OpenClassToEnumClassImpl()
fun getOpenClassToValueClassImpl() = OpenClassToValueClassImpl()
fun getOpenClassToValueClassImplAsAny(): Any = OpenClassToValueClassImpl()
fun getOpenClassToDataClassImpl() = OpenClassToDataClassImpl()
fun getOpenClassToDataClassImplAsAny(): Any = OpenClassToDataClassImpl()
fun getOpenClassToInterfaceImpl() = OpenClassToInterfaceImpl()
fun getOpenClassToInterfaceImplAsAny(): Any = OpenClassToInterfaceImpl()

value class ValueClassInheritsAbstractClass(val x: Int) : InterfaceToAbstractClass
enum class EnumClassInheritsAbstractClass : InterfaceToAbstractClass { ENTRY }

fun getValueClassInheritsAbstractClass() = ValueClassInheritsAbstractClass(42)
fun getValueClassInheritsAbstractClassAsAny(): Any = ValueClassInheritsAbstractClass(42)
fun getEnumClassInheritsAbstractClass() = EnumClassInheritsAbstractClass.ENTRY
fun getEnumClassInheritsAbstractClassAsAny(): Any = EnumClassInheritsAbstractClass.ENTRY

fun getInterfaceToAbstractClass12_1(): InterfaceToAbstractClass1 = object : InterfaceToAbstractClass1, InterfaceToAbstractClass2 {}
fun getInterfaceToAbstractClass12_2(): InterfaceToAbstractClass2 = object : InterfaceToAbstractClass1, InterfaceToAbstractClass2 {}
fun getInterfaceToAbstractClass12AsAny(): Any = object : InterfaceToAbstractClass1, InterfaceToAbstractClass2 {}

fun getInterfaceToAbstractClassAndAbstractClass_1(): InterfaceToAbstractClass1 = object : InterfaceToAbstractClass1, AbstractClass() {}
fun getInterfaceToAbstractClassAndAbstractClass_2(): AbstractClass = object : InterfaceToAbstractClass1, AbstractClass() {}
fun getInterfaceToAbstractClassAndAbstractClassAsAny(): Any = object : InterfaceToAbstractClass1, AbstractClass() {}

open class InterfaceToAbstractClass12Impl : InterfaceToAbstractClass1, InterfaceToAbstractClass2
class InterfaceToAbstractClass12Impl2 : InterfaceToAbstractClass12Impl()
open class InterfaceToAbstractClassAndAbstractClassImpl : InterfaceToAbstractClass1, AbstractClass()
class InterfaceToAbstractClassAndAbstractClassImpl2 : InterfaceToAbstractClassAndAbstractClassImpl()

fun getInterfaceToAbstractClass12Impl() = InterfaceToAbstractClass12Impl()
fun getInterfaceToAbstractClass12ImplAsAny(): Any = InterfaceToAbstractClass12Impl()

fun getInterfaceToAbstractClass12Impl2() = InterfaceToAbstractClass12Impl2()
fun getInterfaceToAbstractClass12Impl2AsAny(): Any = InterfaceToAbstractClass12Impl2()

fun getInterfaceToAbstractClassAndAbstractClassImpl() = InterfaceToAbstractClassAndAbstractClassImpl()
fun getInterfaceToAbstractClassAndAbstractClassImplAsAny(): Any = InterfaceToAbstractClassAndAbstractClassImpl()

fun getInterfaceToAbstractClassAndAbstractClassImpl2() = InterfaceToAbstractClassAndAbstractClassImpl2()
fun getInterfaceToAbstractClassAndAbstractClassImpl2AsAny(): Any = InterfaceToAbstractClassAndAbstractClassImpl2()

fun referenceToInterfaceToAbstractClass12Impl() = check(InterfaceToAbstractClass12Impl::class.simpleName != null)
fun referenceToInterfaceToAbstractClass12Impl2Impl() = check(InterfaceToAbstractClass12Impl2::class.simpleName != null)
fun referenceToInterfaceToAbstractClassAndAbstractClassImpl() = check(InterfaceToAbstractClassAndAbstractClassImpl::class.simpleName != null)
fun referenceToInterfaceToAbstractClassAndAbstractClassImpl2Impl() = check(InterfaceToAbstractClassAndAbstractClassImpl2::class.simpleName != null)

class RemovedInterfaceImpl1 : RemovedInterface {
    override fun abstractFun() = "RemovedInterfaceImpl1.abstractFun"
    override val abstractVal get() = "RemovedInterfaceImpl1.abstractVal"
}

class RemovedInterfaceImpl2 : RemovedInterface {
    override fun abstractFun() = abstractFunWithDefaultImpl()
    override val abstractVal get() = abstractValWithDefaultImpl
}

class RemovedAbstractClassImpl1 : RemovedAbstractClass() {
    override fun abstractFun() = "RemovedAbstractClassImpl1.abstractFun"
    override fun openFun() = "RemovedAbstractClassImpl1.openFun"
    override val abstractVal get() = "RemovedAbstractClassImpl1.abstractVal"
    override val openVal get() = "RemovedAbstractClassImpl1.openVal"
}

class RemovedAbstractClassImpl2 : RemovedAbstractClass() {
    override fun abstractFun() = "${openFun()}:${finalFun()}"
    override val abstractVal get() = "$openVal: $finalVal"
}

class RemovedOpenClassImpl1 : RemovedOpenClass() {
    override fun openFun() = "RemovedOpenClassImpl1.openFun"
    override val openVal get() = "RemovedOpenClassImpl1.openVal"
}

class RemovedOpenClassImpl2 : RemovedOpenClass()

class AbstractClassWithChangedConstructorSignatureImpl() : AbstractClassWithChangedConstructorSignature("Alice")
class OpenClassWithChangedConstructorSignatureImpl() : OpenClassWithChangedConstructorSignature("Bob")
