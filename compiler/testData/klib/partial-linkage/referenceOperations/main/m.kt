import abitestutils.abiTest

fun box() = abiTest {
    // fun
    expectSuccess(true) { createRemovedFunReference() is kotlin.reflect.KFunction<*> }
    expectSuccess("removedFun") { removedFunReferenceName() }
    expectFailure(linkage("Reference to function 'removedFun' can not be evaluated: No function found for symbol '/removedFun'")) { removedFunReferenceHashCode() }
    expectFailure(linkage("Reference to function 'removedFun' can not be evaluated: No function found for symbol '/removedFun'")) { removedFunReferenceEquals() }
    expectFailure(linkage("Reference to function 'removedFun' can not be evaluated: No function found for symbol '/removedFun'")) { removedFunReferenceToString() }
    expectFailure(linkage("Function 'removedFun' can not be called: No function found for symbol '/removedFun'")) { removedFunReferenceInvoke() }

    // constructor
    expectSuccess(true) { createRemovedCtorReference() is kotlin.reflect.KFunction<*> }
    expectSuccess("<init>") { removedCtorReferenceName() }
    expectFailure(linkage("Reference to constructor 'ClassWithRemovedCtor.<init>' can not be evaluated: No constructor found for symbol '/ClassWithRemovedCtor.<init>'")) { removedCtorReferenceHashCode() }
    expectFailure(linkage("Reference to constructor 'ClassWithRemovedCtor.<init>' can not be evaluated: No constructor found for symbol '/ClassWithRemovedCtor.<init>'")) { removedCtorReferenceEquals() }
    expectFailure(linkage("Reference to constructor 'ClassWithRemovedCtor.<init>' can not be evaluated: No constructor found for symbol '/ClassWithRemovedCtor.<init>'")) { removedCtorReferenceToString() }
    expectFailure(linkage("Constructor 'ClassWithRemovedCtor.<init>' can not be called: No constructor found for symbol '/ClassWithRemovedCtor.<init>'")) { removedCtorReferenceInvoke() }

    expectFailure(linkage("Function 'removedGetRegularClassInstance' can not be called: No function found for symbol '/removedGetRegularClassInstance'")) { funReferenceWithErrorInReceiver() }

    // val
    expectSuccess(true) { createRemovedValReference() is kotlin.reflect.KProperty0<*> }
    expectSuccess("removedVal") { removedValReferenceName() }
    expectFailure(linkage("Reference to property 'removedVal' can not be evaluated: No property found for symbol '/removedVal'")) { removedValReferenceHashCode() }
    expectFailure(linkage("Reference to property 'removedVal' can not be evaluated: No property found for symbol '/removedVal'")) { removedValReferenceEquals() }
    expectFailure(linkage("Reference to property 'removedVal' can not be evaluated: No property found for symbol '/removedVal'")) { removedValReferenceToString() }
    expectFailure(linkage("Property accessor 'removedVal.<get-removedVal>' can not be called: No property accessor found for symbol '/removedVal.<get-removedVal>'")) { removedValReferenceInvoke() }
    expectFailure(linkage("Property accessor 'removedVal.<get-removedVal>' can not be called: No property accessor found for symbol '/removedVal.<get-removedVal>'")) { removedValReferenceGet() }

    // var
    expectSuccess(true) { createRemovedVarReference() is kotlin.reflect.KMutableProperty0<*> }
    expectSuccess("removedVar") { removedVarReferenceName() }
    expectFailure(linkage("Reference to property 'removedVar' can not be evaluated: No property found for symbol '/removedVar'")) { removedVarReferenceHashCode() }
    expectFailure(linkage("Reference to property 'removedVar' can not be evaluated: No property found for symbol '/removedVar'")) { removedVarReferenceEquals() }
    expectFailure(linkage("Reference to property 'removedVar' can not be evaluated: No property found for symbol '/removedVar'")) { removedVarReferenceToString() }
    expectFailure(linkage("Property accessor 'removedVar.<get-removedVar>' can not be called: No property accessor found for symbol '/removedVar.<get-removedVar>'")) { removedVarReferenceInvoke() }
    expectFailure(linkage("Property accessor 'removedVar.<get-removedVar>' can not be called: No property accessor found for symbol '/removedVar.<get-removedVar>'")) { removedVarReferenceGet() }
    expectFailure(linkage("Property accessor 'removedVar.<set-removedVar>' can not be called: No property accessor found for symbol '/removedVar.<set-removedVar>'")) { removedVarReferenceSet() }

    // var by ::var
    expectSuccess(true) { createRemovedVarDelegateReference() is kotlin.reflect.KMutableProperty0<*> }
    expectSuccess("removedVarDelegate") { removedVarDelegateReferenceName() }
    expectSuccess { removedVarDelegateReferenceHashCode(); "OK" }
    expectSuccess(false) { removedVarDelegateReferenceEquals() }
    expectSuccess { removedVarDelegateReferenceToString(); "OK" }
    expectFailure(linkage("Property accessor 'removedVar.<get-removedVar>' can not be called: No property accessor found for symbol '/removedVar.<get-removedVar>'")) { removedVarDelegateReferenceInvoke() }
    expectFailure(linkage("Property accessor 'removedVar.<get-removedVar>' can not be called: No property accessor found for symbol '/removedVar.<get-removedVar>'")) { removedVarDelegateReferenceGet() }
    expectFailure(linkage("Property accessor 'removedVar.<set-removedVar>' can not be called: No property accessor found for symbol '/removedVar.<set-removedVar>'")) { removedVarDelegateReferenceSet() }
}
