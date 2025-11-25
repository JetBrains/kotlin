import abitestutils.abiTest

fun box() = abiTest {
    val stableClass = StableClass()
    val stableClassInner = stableClass.Inner()
    val sfh = StableFunctionsHolder()
    val classWithChangedMembers = ClassWithChangedMembers()

    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { referenceRemovedClassReference() }
    expectFailure(linkage("Reference to constructor 'RemovedClass.<init>' can not be evaluated: Expression uses unlinked class symbol '/RemovedClass'")) { referenceRemovedClassConstructorReference() }
    expectFailure(linkage("Reference to property 'p1' can not be evaluated: Expression uses unlinked class symbol '/RemovedClass'")) { referenceRemovedClassProperty1Reference() }
    expectFailure(linkage("Reference to function 'f1' can not be evaluated: Expression uses unlinked class symbol '/RemovedClass'")) { referenceRemovedClassFunction1Reference() }

    expectFailure(linkage("Reference to class 'RemovedClassImpl' can not be evaluated: Expression uses unlinked class symbol '/RemovedClass' (via class 'RemovedClassImpl')")) { referenceRemovedClassImplReference() }
    expectFailure(linkage("Reference to constructor 'RemovedClassImpl.<init>' can not be evaluated: Expression uses unlinked class symbol '/RemovedClass' (via class 'RemovedClassImpl')")) { referenceRemovedClassImplConstructorReference() }
    expectFailure(linkage("Reference to property 'p1' can not be evaluated: Expression uses unlinked class symbol '/RemovedClass' (via class 'RemovedClassImpl')")) { referenceRemovedClassImplProperty1Reference() }
    expectFailure(linkage("Reference to property 'p2' can not be evaluated: Expression uses unlinked class symbol '/RemovedClass' (via class 'RemovedClassImpl')")) { referenceRemovedClassImplProperty2Reference() }
    expectFailure(linkage("Reference to function 'f1' can not be evaluated: Expression uses unlinked class symbol '/RemovedClass' (via class 'RemovedClassImpl')")) { referenceRemovedClassImplFunction1Reference() }
    expectFailure(linkage("Reference to function 'f2' can not be evaluated: Expression uses unlinked class symbol '/RemovedClass' (via class 'RemovedClassImpl')")) { referenceRemovedClassImplFunction2Reference() }

    expectFailure(linkage("Reference to class 'RemovedInterface' can not be evaluated: No class found for symbol '/RemovedInterface'")) { referenceRemovedInterfaceReference() }
    expectFailure(linkage("Reference to property 'p1' can not be evaluated: Expression uses unlinked class symbol '/RemovedInterface'")) { referenceRemovedInterfaceProperty1Reference() }
    expectFailure(linkage("Reference to property 'p2' can not be evaluated: Expression uses unlinked class symbol '/RemovedInterface'")) { referenceRemovedInterfaceProperty2Reference() }
    expectFailure(linkage("Reference to function 'f1' can not be evaluated: Expression uses unlinked class symbol '/RemovedInterface'")) { referenceRemovedInterfaceFunction1Reference() }
    expectFailure(linkage("Reference to function 'f2' can not be evaluated: Expression uses unlinked class symbol '/RemovedInterface'")) { referenceRemovedInterfaceFunction2Reference() }

    expectFailure(linkage("Reference to class 'RemovedInterfaceImpl' can not be evaluated: Expression uses unlinked class symbol '/RemovedInterface' (via class 'RemovedInterfaceImpl')")) { referenceRemovedInterfaceImplReference() }
    expectFailure(linkage("Reference to property 'p1' can not be evaluated: Expression uses unlinked class symbol '/RemovedInterface' (via class 'RemovedInterfaceImpl')")) { referenceRemovedInterfaceImplProperty1Reference() }
    expectFailure(linkage("Reference to property 'p2' can not be evaluated: Expression uses unlinked class symbol '/RemovedInterface' (via class 'RemovedInterfaceImpl')")) { referenceRemovedInterfaceImplProperty2Reference() }
    expectFailure(linkage("Reference to property 'p3' can not be evaluated: Expression uses unlinked class symbol '/RemovedInterface' (via class 'RemovedInterfaceImpl')")) { referenceRemovedInterfaceImplProperty3Reference() }
    expectFailure(linkage("Reference to function 'f1' can not be evaluated: Expression uses unlinked class symbol '/RemovedInterface' (via class 'RemovedInterfaceImpl')")) { referenceRemovedInterfaceImplFunction1Reference() }
    expectFailure(linkage("Reference to function 'f2' can not be evaluated: Expression uses unlinked class symbol '/RemovedInterface' (via class 'RemovedInterfaceImpl')")) { referenceRemovedInterfaceImplFunction2Reference() }
    expectFailure(linkage("Reference to function 'f3' can not be evaluated: Expression uses unlinked class symbol '/RemovedInterface' (via class 'RemovedInterfaceImpl')")) { referenceRemovedInterfaceImplFunction3Reference() }

    expectSuccess("<init>") { referenceStableClassConstructor() }
    expectSuccess("foo") { referenceStableClassMemberFunctionWithoutDispatchReceiver() }
    expectSuccess("foo") { referenceStableClassMemberFunctionWithDispatchReceiver(stableClass) }
    expectSuccess("<init>") { referenceStableClassInnerConstructorWithoutDispatchReceiver() }
    expectSuccess("<init>") { referenceStableClassInnerConstructorWithDispatchReceiver(stableClass) }
    expectSuccess("bar") { referenceStableClassInnerMemberFunctionWithoutDispatchReceiver() }
    expectSuccess("bar") { referenceStableClassInnerMemberFunctionWithDispatchReceiver(stableClassInner) }

    if (!testMode.isJs) {
        expectFailure(linkage("Reference to function 'removedFun' can not be evaluated: No function found for symbol '/ClassWithChangedMembers.removedFun'")) { referenceRemovedFunFromClass() }
        expectFailure(linkage("Reference to function 'changedFun' can not be evaluated: No function found for symbol '/ClassWithChangedMembers.changedFun'")) { referenceChangedFunFromClass() }
        expectFailure(linkage("Reference to function 'removedFun' can not be evaluated: No function found for symbol '/InterfaceWithChangedMembers.removedFun'")) { referenceRemovedFunFromInterface() }
        expectFailure(linkage("Reference to function 'changedFun' can not be evaluated: No function found for symbol '/InterfaceWithChangedMembers.changedFun'")) { referenceChangedFunFromInterface() }
    }

    expectSuccess("<init>") { referenceNestedToInnerConstructorWithoutDispatchReceiver() }
    expectSuccess("<init>") { referenceInnerToNestedConstructorWithoutDispatchReceiver() }
    expectSuccess("<init>") { referenceInnerToNestedConstructorWithDispatchReceiver(classWithChangedMembers) }

    expectFailure(linkage("Constructor 'NestedToInner.<init>' can not be called: The call site has 1 less value argument(s) than the constructor requires. Those arguments are missing: x")) { invokeNestedToInnerConstructorWithoutDispatchReceiver() }
    expectFailure(linkage("Constructor 'InnerToNested.<init>' can not be called: The call site provides 1 more value argument(s) than the constructor expects")) { invokeInnerToNestedConstructorWithoutDispatchReceiver(classWithChangedMembers) }
    expectFailure(linkage("Constructor 'InnerToNested.<init>' can not be called: The call site provides 1 more value argument(s) than the constructor expects")) { invokeInnerToNestedConstructorWithDispatchReceiver(classWithChangedMembers) }

    expectFailure(linkage("Reference to function 'functionWithUnlinkedParameter' can not be evaluated: Expression uses unlinked class symbol '/RemovedClass'")) { referenceFunctionWithUnlinkedParameter() }
    expectFailure(linkage("Reference to function 'functionWithUnlinkedReturnValue' can not be evaluated: Expression uses unlinked class symbol '/RemovedClass'")) { referenceFunctionWithUnlinkedReturnValue() }
    expectFailure(linkage("Reference to function 'functionWithRemovedTypeParameter' can not be evaluated: Expression uses unlinked class symbol '/RemovedClass' (via class 'RemovedClassImpl')")) { referenceFunctionWithRemovedTypeParameter() }

    expectSuccess("foo") { referencingMemberFunctionFoo(sfh) }
    expectSuccess("bar") { referencingMemberFunctionBar(sfh) }
    expectSuccess("baz") { referencingMemberFunctionBaz(sfh) }
    expectSuccess { referencingAnyEquals(Any()) }
    expectSuccess { referencingStableClassWithEquals(StableClassWithEquals(42)) }
}
