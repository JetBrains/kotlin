import abitestutils.abiTest

fun box() = abiTest {
    val ii: InterfaceImpl = InterfaceImpl()
    val i: Interface = ii
    val aci: AbstractClassImpl = AbstractClassImpl()
    val ac: AbstractClass = aci
    val oci: OpenClassImpl = OpenClassImpl()
    val oc: OpenClass = oci

    expectSuccess("memberOperatorsToNonOperators: a=Alice,b=Bob") { memberOperatorsToNonOperators("a" to "Alice", "b" to "Bob") }
    expectSuccess("extensionOperatorsToNonOperators: a=Alice,b=Bob") { extensionOperatorsToNonOperators("a" to "Alice", "b" to "Bob") }
    expectSuccess("memberNonOperatorsToOperators: a=Alice,b=Bob") { memberNonOperatorsToOperators("a" to "Alice", "b" to "Bob") }
    expectSuccess("extensionNonOperatorsToOperators: a=Alice,b=Bob") { extensionNonOperatorsToOperators("a" to "Alice", "b" to "Bob") }

    expectSuccess(3) { memberNonInfixToInfix(1, 2) }
    expectSuccess(3) { extensionNonInfixToInfix(1, 2) }
    expectSuccess(3) { memberInfixToNonInfix(1, 2) }
    expectSuccess(3) { extensionInfixToNonInfix(1, 2) }

    expectSuccess(6) { nonTailrecToTailrec(3) }
    expectSuccess(6) { tailrecToNonTailrec(3) }

    expectFailure(linkage("Function 'removedDefaultValue' can not be called: The call site provides less value arguments (1) than the function requires (2)")) { removedDefaultValueInFunction(1) }
    expectFailure(linkage("Constructor 'RemovedDefaultValueInConstructor.<init>' can not be called: The call site provides less value arguments (1) than the constructor requires (2)")) { removedDefaultValueInConstructor(1) }

    expectSuccess(-1) { suspendToNonSuspendFunction1(1) }
    expectSuccess(-2) { suspendToNonSuspendFunction2(2) }
    expectSuccess(-3) { suspendToNonSuspendFunction3(3) }
    expectFailure(linkage("Function 'nonSuspendToSuspendFunction' can not be called: Suspend function can be called only from a coroutine or another suspend function")) { nonSuspendToSuspendFunction1(4) }
    expectSuccess(-5) { nonSuspendToSuspendFunction2(5) }
    expectFailure(linkage("Function 'nonSuspendToSuspendFunction' can not be called: Suspend function can be called only from a coroutine or another suspend function")) { nonSuspendToSuspendFunction3(6) }
    expectSuccess(-7) { nonSuspendToSuspendFunction4(7) }

    expectFailure(linkage("Abstract function 'suspendToNonSuspendFunction' is not implemented in non-abstract class 'InterfaceImpl'")) { suspendToNonSuspendFunctionInInterface(i, 1) }
    expectFailure(linkage("Function 'nonSuspendToSuspendFunction' can not be called: Suspend function can be called only from a coroutine or another suspend function")) { nonSuspendToSuspendFunctionInInterface(i, 2) }
    expectSuccess("InterfaceImpl.suspendToNonSuspendFunction(3)") { suspendToNonSuspendFunctionInInterfaceImpl(ii, 3) }
    expectSuccess("InterfaceImpl.nonSuspendToSuspendFunction(4)") { nonSuspendToSuspendFunctionInInterfaceImpl(ii, 4) }

    expectFailure(linkage("Abstract function 'suspendToNonSuspendFunction' is not implemented in non-abstract class 'AbstractClassImpl'")) { suspendToNonSuspendFunctionInAbstractClass(ac, 5) }
    expectFailure(linkage("Function 'nonSuspendToSuspendFunction' can not be called: Suspend function can be called only from a coroutine or another suspend function")) { nonSuspendToSuspendFunctionInAbstractClass(ac, 6) }
    expectSuccess("AbstractClassImpl.suspendToNonSuspendFunction(7)") { suspendToNonSuspendFunctionInAbstractClassImpl(aci, 7) }
    expectSuccess("AbstractClassImpl.nonSuspendToSuspendFunction(8)") { nonSuspendToSuspendFunctionInAbstractClassImpl(aci, 8) }

    expectSuccess("OpenClassV2.suspendToNonSuspendFunction(9)") { suspendToNonSuspendFunctionInOpenClass(oc, 9) } // Function of the base class is called instead of overridden function in inherited class.
    expectFailure(linkage("Function 'nonSuspendToSuspendFunction' can not be called: Suspend function can be called only from a coroutine or another suspend function")) { nonSuspendToSuspendFunctionInOpenClass(oc, 10) }
    expectSuccess("OpenClassImpl.suspendToNonSuspendFunction(11)") { suspendToNonSuspendFunctionInOpenClassImpl(oci, 11) }
    expectSuccess("OpenClassImpl.nonSuspendToSuspendFunction(12)") { nonSuspendToSuspendFunctionInOpenClassImpl(oci, 12) }
    expectSuccess("OpenClassV2.suspendToNonSuspendFunctionWithDelegation(13) called from OpenClassImpl.suspendToNonSuspendFunctionWithDelegation(13)") { suspendToNonSuspendFunctionWithDelegation(oci, 13) }
    expectFailure(linkage("Function 'nonSuspendToSuspendFunctionWithDelegation' can not be called: Suspend function can be called only from a coroutine or another suspend function")) { nonSuspendToSuspendFunctionWithDelegation(oci, 14) }
}
