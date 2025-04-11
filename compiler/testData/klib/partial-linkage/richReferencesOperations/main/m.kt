import abitestutils.abiTest

fun box() = abiTest {
    // fun
    expectSuccess(true) { createRemovedFunReference() is kotlin.reflect.KFunction<*> }
    expectFailure(linkage("Reference to function 'removedFun' can not be evaluated: No function found for symbol '/removedFun'")) { removedFunReferenceName() }
    expectFailure(linkage("Reference to function 'removedFun' can not be evaluated: No function found for symbol '/removedFun'")) { removedFunReferenceReturnType() }
    expectFailure(linkage("Reference to function 'removedFun' can not be evaluated: No function found for symbol '/removedFun'")) { removedFunReferenceHashCode() }
    expectFailure(linkage("Reference to function 'removedFun' can not be evaluated: No function found for symbol '/removedFun'")) { removedFunReferenceEquals() }
    expectFailure(linkage("Reference to function 'removedFun' can not be evaluated: No function found for symbol '/removedFun'")) { removedFunReferenceToString() }
    expectFailure(linkage("Function 'removedFun' can not be called: No function found for symbol '/removedFun'")) { removedFunReferenceInvoke() }

    // inline fun
    expectSuccess(true) { createRemovedInlineFunReference() is kotlin.reflect.KFunction<*> }
    expectFailure(linkage("Reference to function 'removedInlineFun' can not be evaluated: No function found for symbol '/removedInlineFun'")) { removedInlineFunReferenceName() }
    expectFailure(linkage("Reference to function 'removedInlineFun' can not be evaluated: No function found for symbol '/removedInlineFun'")) { removedInlineFunReferenceReturnType() }
    expectFailure(linkage("Reference to function 'removedInlineFun' can not be evaluated: No function found for symbol '/removedInlineFun'")) { removedInlineFunReferenceHashCode() }
    expectFailure(linkage("Reference to function 'removedInlineFun' can not be evaluated: No function found for symbol '/removedInlineFun'")) { removedInlineFunReferenceEquals() }
    expectFailure(linkage("Reference to function 'removedInlineFun' can not be evaluated: No function found for symbol '/removedInlineFun'")) { removedInlineFunReferenceToString() }
    // This test should start to pass with klib inlining enabled - KT-64570.
    expectFailure(linkage("Function 'removedInlineFun' can not be called: No function found for symbol '/removedInlineFun'")) { removedInlineFunReferenceInvoke() }

    // constructor
    expectSuccess(true) { createRemovedCtorReference() is kotlin.reflect.KFunction<*> }
    expectFailure(linkage("Reference to constructor 'ClassWithRemovedCtor.<init>' can not be evaluated: No constructor found for symbol '/ClassWithRemovedCtor.<init>'")) { removedCtorReferenceName() }
    expectFailure(linkage("Reference to constructor 'ClassWithRemovedCtor.<init>' can not be evaluated: No constructor found for symbol '/ClassWithRemovedCtor.<init>'")) { removedCtorReferenceReturnType() }
    expectFailure(linkage("Reference to constructor 'ClassWithRemovedCtor.<init>' can not be evaluated: No constructor found for symbol '/ClassWithRemovedCtor.<init>'")) { removedCtorReferenceHashCode() }
    expectFailure(linkage("Reference to constructor 'ClassWithRemovedCtor.<init>' can not be evaluated: No constructor found for symbol '/ClassWithRemovedCtor.<init>'")) { removedCtorReferenceEquals() }
    expectFailure(linkage("Reference to constructor 'ClassWithRemovedCtor.<init>' can not be evaluated: No constructor found for symbol '/ClassWithRemovedCtor.<init>'")) { removedCtorReferenceToString() }
    expectFailure(linkage("Constructor 'ClassWithRemovedCtor.<init>' can not be called: No constructor found for symbol '/ClassWithRemovedCtor.<init>'")) { removedCtorReferenceInvoke() }

    expectFailure(linkage("Function 'removedGetRegularClassInstance' can not be called: No function found for symbol '/removedGetRegularClassInstance'")) { funReferenceWithErrorInReceiver() }

    // val
    expectSuccess(true) { createRemovedValReference() is kotlin.reflect.KProperty0<*> }
    expectFailure(linkage("Reference to property 'removedVal' can not be evaluated: No property found for symbol '/removedVal'")) { removedValReferenceName() }
    expectFailure(linkage("Reference to property 'removedVal' can not be evaluated: No property found for symbol '/removedVal'")) { removedValReferenceReturnType() }
    expectFailure(linkage("Reference to property 'removedVal' can not be evaluated: No property found for symbol '/removedVal'")) { removedValReferenceHashCode() }
    expectFailure(linkage("Reference to property 'removedVal' can not be evaluated: No property found for symbol '/removedVal'")) { removedValReferenceEquals() }
    expectFailure(linkage("Reference to property 'removedVal' can not be evaluated: No property found for symbol '/removedVal'")) { removedValReferenceToString() }
    expectFailure(linkage("Property accessor 'removedVal.<get-removedVal>' can not be called: No property accessor found for symbol '/removedVal.<get-removedVal>'")) { removedValReferenceInvoke() }
    expectFailure(linkage("Property accessor 'removedVal.<get-removedVal>' can not be called: No property accessor found for symbol '/removedVal.<get-removedVal>'")) { removedValReferenceGet() }

    // inline val
    expectSuccess(true) { createRemovedInlineValReference() is kotlin.reflect.KProperty0<*> }
    expectFailure(linkage("Reference to property 'removedInlineVal' can not be evaluated: No property found for symbol '/removedInlineVal'")) { removedInlineValReferenceName() }
    expectFailure(linkage("Reference to property 'removedInlineVal' can not be evaluated: No property found for symbol '/removedInlineVal'")) { removedInlineValReferenceReturnType() }
    expectFailure(linkage("Reference to property 'removedInlineVal' can not be evaluated: No property found for symbol '/removedInlineVal'")) { removedInlineValReferenceHashCode() }
    expectFailure(linkage("Reference to property 'removedInlineVal' can not be evaluated: No property found for symbol '/removedInlineVal'")) { removedInlineValReferenceEquals() }
    expectFailure(linkage("Reference to property 'removedInlineVal' can not be evaluated: No property found for symbol '/removedInlineVal'")) { removedInlineValReferenceToString() }
    // Those tests should start to pass with klib inlining enabled - KT-64570.
    expectFailure(linkage("Property accessor 'removedInlineVal.<get-removedInlineVal>' can not be called: No property accessor found for symbol '/removedInlineVal.<get-removedInlineVal>'")) { removedInlineValReferenceInvoke() }
    expectFailure(linkage("Property accessor 'removedInlineVal.<get-removedInlineVal>' can not be called: No property accessor found for symbol '/removedInlineVal.<get-removedInlineVal>'")) { removedInlineValReferenceGet() }

    // var
    expectSuccess(true) { createRemovedVarReference() is kotlin.reflect.KMutableProperty0<*> }
    expectFailure(linkage("Reference to property 'removedVar' can not be evaluated: No property found for symbol '/removedVar'")) { removedVarReferenceName() }
    expectFailure(linkage("Reference to property 'removedVar' can not be evaluated: No property found for symbol '/removedVar'")) { removedVarReferenceReturnType() }
    expectFailure(linkage("Reference to property 'removedVar' can not be evaluated: No property found for symbol '/removedVar'")) { removedVarReferenceHashCode() }
    expectFailure(linkage("Reference to property 'removedVar' can not be evaluated: No property found for symbol '/removedVar'")) { removedVarReferenceEquals() }
    expectFailure(linkage("Reference to property 'removedVar' can not be evaluated: No property found for symbol '/removedVar'")) { removedVarReferenceToString() }
    expectFailure(linkage("Property accessor 'removedVar.<get-removedVar>' can not be called: No property accessor found for symbol '/removedVar.<get-removedVar>'")) { removedVarReferenceInvoke() }
    expectFailure(linkage("Property accessor 'removedVar.<get-removedVar>' can not be called: No property accessor found for symbol '/removedVar.<get-removedVar>'")) { removedVarReferenceGet() }
    expectFailure(linkage("Property accessor 'removedVar.<set-removedVar>' can not be called: No property accessor found for symbol '/removedVar.<set-removedVar>'")) { removedVarReferenceSet() }

    // inline var
    expectSuccess(true) { createRemovedInlineVarReference() is kotlin.reflect.KMutableProperty0<*> }
    expectFailure(linkage("Reference to property 'removedInlineVar' can not be evaluated: No property found for symbol '/removedInlineVar'")) { removedInlineVarReferenceName() }
    expectFailure(linkage("Reference to property 'removedInlineVar' can not be evaluated: No property found for symbol '/removedInlineVar'")) { removedInlineVarReferenceReturnType() }
    expectFailure(linkage("Reference to property 'removedInlineVar' can not be evaluated: No property found for symbol '/removedInlineVar'")) { removedInlineVarReferenceHashCode() }
    expectFailure(linkage("Reference to property 'removedInlineVar' can not be evaluated: No property found for symbol '/removedInlineVar'")) { removedInlineVarReferenceEquals() }
    expectFailure(linkage("Reference to property 'removedInlineVar' can not be evaluated: No property found for symbol '/removedInlineVar'")) { removedInlineVarReferenceToString() }
    // Those tests should start to pass with klib inlining enabled - KT-64570.
    expectFailure(linkage("Property accessor 'removedInlineVar.<get-removedInlineVar>' can not be called: No property accessor found for symbol '/removedInlineVar.<get-removedInlineVar>'")) { removedInlineVarReferenceInvoke() }
    expectFailure(linkage("Property accessor 'removedInlineVar.<get-removedInlineVar>' can not be called: No property accessor found for symbol '/removedInlineVar.<get-removedInlineVar>'")) { removedInlineVarReferenceGet() }
    expectFailure(linkage("Property accessor 'removedInlineVar.<set-removedInlineVar>' can not be called: No property accessor found for symbol '/removedInlineVar.<set-removedInlineVar>'")) { removedInlineVarReferenceSet() }

    // var by ::var
    expectSuccess(true) { createRemovedVarDelegateReference() is kotlin.reflect.KMutableProperty0<*> }
    expectSuccess("removedVarDelegate") { removedVarDelegateReferenceName() }
    expectSuccess { removedVarDelegateReferenceReturnType(); "OK" }
    expectSuccess { removedVarDelegateReferenceHashCode(); "OK" }
    expectSuccess(false) { removedVarDelegateReferenceEquals() }
    expectSuccess { removedVarDelegateReferenceToString(); "OK" }
    expectFailure(linkage("Property accessor 'removedVar.<get-removedVar>' can not be called: No property accessor found for symbol '/removedVar.<get-removedVar>'")) { removedVarDelegateReferenceInvoke() }
    expectFailure(linkage("Property accessor 'removedVar.<get-removedVar>' can not be called: No property accessor found for symbol '/removedVar.<get-removedVar>'")) { removedVarDelegateReferenceGet() }
    expectFailure(linkage("Property accessor 'removedVar.<set-removedVar>' can not be called: No property accessor found for symbol '/removedVar.<set-removedVar>'")) { removedVarDelegateReferenceSet() }
}
