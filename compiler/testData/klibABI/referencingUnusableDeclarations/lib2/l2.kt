class RemovedClassImpl : RemovedClass() {
    val p2 = "p2"
    fun f2() = "f2"
}

class RemovedInterfaceImpl : RemovedInterface {
    override val p1 = "p1"
    val p3 = "p3"
    override fun f1() = "f1"
    fun f3() = "f3"
}

fun functionWithUnlinkedParameter(p: RemovedClass?) = "functionWithUnlinkedParameter"
fun functionWithUnlinkedReturnValue(): RemovedClass = TODO("functionWithUnlinkedReturnValue")
fun <T : RemovedClass> functionWithRemovedTypeParameter(a: Any?): T = TODO("functionWithRemovedTypeParameter")

fun referenceRemovedClassReference(): String = RemovedClass::class.simpleName.orEmpty()
fun referenceRemovedClassConstructorReference(): String = ::RemovedClass.name
fun referenceRemovedClassProperty1Reference(): String = RemovedClass::p1.name
fun referenceRemovedClassFunction1Reference(): String = RemovedClass::f1.name

fun referenceRemovedClassImplReference(): String = RemovedClassImpl::class.simpleName.orEmpty()
fun referenceRemovedClassImplConstructorReference(): String = ::RemovedClassImpl.name
fun referenceRemovedClassImplProperty1Reference(): String = RemovedClassImpl::p1.name
fun referenceRemovedClassImplProperty2Reference(): String = RemovedClassImpl::p2.name
fun referenceRemovedClassImplFunction1Reference(): String = RemovedClassImpl::f1.name
fun referenceRemovedClassImplFunction2Reference(): String = RemovedClassImpl::f2.name

fun referenceRemovedInterfaceReference(): String = RemovedInterface::class.simpleName.orEmpty()
fun referenceRemovedInterfaceProperty1Reference(): String = RemovedInterface::p1.name
fun referenceRemovedInterfaceProperty2Reference(): String = RemovedInterface::p2.name
fun referenceRemovedInterfaceFunction1Reference(): String = RemovedInterface::f1.name
fun referenceRemovedInterfaceFunction2Reference(): String = RemovedInterface::f2.name

fun referenceRemovedInterfaceImplReference(): String = RemovedInterfaceImpl::class.simpleName.orEmpty()
fun referenceRemovedInterfaceImplProperty1Reference(): String = RemovedInterfaceImpl::p1.name
fun referenceRemovedInterfaceImplProperty2Reference(): String = RemovedInterfaceImpl::p2.name
fun referenceRemovedInterfaceImplProperty3Reference(): String = RemovedInterfaceImpl::p3.name
fun referenceRemovedInterfaceImplFunction1Reference(): String = RemovedInterfaceImpl::f1.name
fun referenceRemovedInterfaceImplFunction2Reference(): String = RemovedInterfaceImpl::f2.name
fun referenceRemovedInterfaceImplFunction3Reference(): String = RemovedInterfaceImpl::f3.name

fun referenceRemovedFunFromClass(): String = ClassWithChangedMembers::removedFun.name
fun referenceChangedFunFromClass(): String = ClassWithChangedMembers::changedFun.name
fun referenceRemovedFunFromInterface(): String = InterfaceWithChangedMembers::removedFun.name
fun referenceChangedFunFromInterface(): String = InterfaceWithChangedMembers::changedFun.name

fun referenceFunctionWithUnlinkedParameter(): String = ::functionWithUnlinkedParameter.name
fun referenceFunctionWithUnlinkedReturnValue(): String = ::functionWithUnlinkedReturnValue.name
fun referenceFunctionWithRemovedTypeParameter(): String {
    /*
     * The syntax of function references does not allow explicitly specifying type arguments.
     * The compiler will report "Not enough information to infer type variable T" error on
     * `::functionWithRemovedTypeParameter` statement. So let's put the reference into
     * the context with evident type arguments.
     */
    return listOf<Any?>(null).map<Any?, RemovedClassImpl>(::functionWithRemovedTypeParameter).joinToString()
}
