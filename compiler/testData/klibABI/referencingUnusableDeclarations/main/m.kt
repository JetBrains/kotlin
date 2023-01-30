import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Reference to class 'RemovedClass' can not be evaluated: No class found for symbol '/RemovedClass'")) { referenceRemovedClassReference() }
    expectFailure(linkage("Reference to constructor 'RemovedClass.<init>' can not be evaluated: No constructor found for symbol '/RemovedClass.<init>'")) { referenceRemovedClassConstructorReference() }
    expectFailure(linkage("Reference to property 'p1' can not be evaluated: No property found for symbol '/RemovedClass.p1'")) { referenceRemovedClassProperty1Reference() }
    expectFailure(linkage("Reference to function 'f1' can not be evaluated: No function found for symbol '/RemovedClass.f1'")) { referenceRemovedClassFunction1Reference() }

    expectFailure(linkage("Reference to class 'RemovedClassImpl' can not be evaluated: Expression uses unlinked class symbol '/RemovedClass' (via class 'RemovedClassImpl')")) { referenceRemovedClassImplReference() }
    expectFailure(linkage("Reference to constructor 'RemovedClassImpl.<init>' can not be evaluated: Class 'RemovedClassImpl' uses unlinked class symbol '/RemovedClass'")) { referenceRemovedClassImplConstructorReference() }
    expectFailure(linkage("Reference to property 'p1' can not be evaluated: No property found for symbol '/RemovedClassImpl.p1'")) { referenceRemovedClassImplProperty1Reference() }
    expectFailure(linkage("Reference to property 'p2' can not be evaluated: Dispatch receiver class 'RemovedClassImpl' uses unlinked class symbol '/RemovedClass'")) { referenceRemovedClassImplProperty2Reference() }
    expectFailure(linkage("Reference to function 'f1' can not be evaluated: No function found for symbol '/RemovedClassImpl.f1'")) { referenceRemovedClassImplFunction1Reference() }
    expectFailure(linkage("Reference to function 'f2' can not be evaluated: Dispatch receiver class 'RemovedClassImpl' uses unlinked class symbol '/RemovedClass'")) { referenceRemovedClassImplFunction2Reference() }

    expectFailure(linkage("Reference to class 'RemovedInterface' can not be evaluated: No class found for symbol '/RemovedInterface'")) { referenceRemovedInterfaceReference() }
    expectFailure(linkage("Reference to property 'p1' can not be evaluated: No property found for symbol '/RemovedInterface.p1'")) { referenceRemovedInterfaceProperty1Reference() }
    expectFailure(linkage("Reference to property 'p2' can not be evaluated: No property found for symbol '/RemovedInterface.p2'")) { referenceRemovedInterfaceProperty2Reference() }
    expectFailure(linkage("Reference to function 'f1' can not be evaluated: No function found for symbol '/RemovedInterface.f1'")) { referenceRemovedInterfaceFunction1Reference() }
    expectFailure(linkage("Reference to function 'f2' can not be evaluated: No function found for symbol '/RemovedInterface.f2'")) { referenceRemovedInterfaceFunction2Reference() }

    expectFailure(linkage("Reference to class 'RemovedInterfaceImpl' can not be evaluated: Expression uses unlinked class symbol '/RemovedInterface' (via class 'RemovedInterfaceImpl')")) { referenceRemovedInterfaceImplReference() }
    expectFailure(linkage("Reference to property 'p1' can not be evaluated: Dispatch receiver class 'RemovedInterfaceImpl' uses unlinked class symbol '/RemovedInterface'")) { referenceRemovedInterfaceImplProperty1Reference() }
    expectFailure(linkage("Reference to property 'p2' can not be evaluated: No property found for symbol '/RemovedInterfaceImpl.p2'")) { referenceRemovedInterfaceImplProperty2Reference() }
    expectFailure(linkage("Reference to property 'p3' can not be evaluated: Dispatch receiver class 'RemovedInterfaceImpl' uses unlinked class symbol '/RemovedInterface'")) { referenceRemovedInterfaceImplProperty3Reference() }
    expectFailure(linkage("Reference to function 'f1' can not be evaluated: Dispatch receiver class 'RemovedInterfaceImpl' uses unlinked class symbol '/RemovedInterface'")) { referenceRemovedInterfaceImplFunction1Reference() }
    expectFailure(linkage("Reference to function 'f2' can not be evaluated: No function found for symbol '/RemovedInterfaceImpl.f2'")) { referenceRemovedInterfaceImplFunction2Reference() }
    expectFailure(linkage("Reference to function 'f3' can not be evaluated: Dispatch receiver class 'RemovedInterfaceImpl' uses unlinked class symbol '/RemovedInterface'")) { referenceRemovedInterfaceImplFunction3Reference() }

    expectFailure(linkage("Reference to function 'removedFun' can not be evaluated: No function found for symbol '/ClassWithChangedMembers.removedFun'")) { referenceRemovedFunFromClass() }
    expectFailure(linkage("Reference to function 'changedFun' can not be evaluated: No function found for symbol '/ClassWithChangedMembers.changedFun'")) { referenceChangedFunFromClass() }
    expectFailure(linkage("Reference to function 'removedFun' can not be evaluated: No function found for symbol '/InterfaceWithChangedMembers.removedFun'")) { referenceRemovedFunFromInterface() }
    expectFailure(linkage("Reference to function 'changedFun' can not be evaluated: No function found for symbol '/InterfaceWithChangedMembers.changedFun'")) { referenceChangedFunFromInterface() }

    expectFailure(linkage("Reference to function 'functionWithUnlinkedParameter' can not be evaluated: Function uses unlinked class symbol '/RemovedClass'")) { referenceFunctionWithUnlinkedParameter() }
    expectFailure(linkage("Reference to function 'functionWithUnlinkedReturnValue' can not be evaluated: Function uses unlinked class symbol '/RemovedClass'")) { referenceFunctionWithUnlinkedReturnValue() }
    expectFailure(linkage("Reference to function 'functionWithRemovedTypeParameter' can not be evaluated: Function uses unlinked class symbol '/RemovedClass' (via type parameter 'T')")) { referenceFunctionWithRemovedTypeParameter() }
}
