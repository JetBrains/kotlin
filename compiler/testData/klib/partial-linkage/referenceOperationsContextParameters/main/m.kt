import abitestutils.abiTest

fun box() = abiTest {
    // fun
    expectSuccess(true) { createRemovedCtxFunReference() is kotlin.reflect.KFunction<*> }
    expectSuccess("removedCtxFun") { removedCtxFunReferenceName() }
    // On JS a callable reference is a plain function object: hashCode/equals/toString do not evaluate the unlinked target and succeed
    if (testMode.isJs) {
        expectSuccess { removedCtxFunReferenceHashCode(); "OK" }
        expectSuccess(false) { removedCtxFunReferenceEquals() }
        expectSuccess { removedCtxFunReferenceToString(); "OK" }
    } else {
        expectFailure(linkage("Reference to function 'removedCtxFun' can not be evaluated: No function found for symbol '/removedCtxFun'")) { removedCtxFunReferenceHashCode() }
        expectFailure(linkage("Reference to function 'removedCtxFun' can not be evaluated: No function found for symbol '/removedCtxFun'")) { removedCtxFunReferenceEquals() }
        expectFailure(linkage("Reference to function 'removedCtxFun' can not be evaluated: No function found for symbol '/removedCtxFun'")) { removedCtxFunReferenceToString() }
    }
    expectFailure(linkage("Function 'removedCtxFun' can not be called: No function found for symbol '/removedCtxFun'")) { removedCtxFunReferenceInvoke() }

    // member fun
    expectSuccess("removedMemberCtxFun") { removedMemberCtxFunReferenceName() }
    expectFailure(linkage("Function 'removedMemberCtxFun' can not be called: No function found for symbol '/ObjectWithRemovedCtxFun.removedMemberCtxFun'")) { removedMemberCtxFunReferenceInvoke() }

    // val
    expectSuccess(true) { createRemovedCtxValReference() is kotlin.reflect.KProperty0<*> }
    expectSuccess("removedCtxVal") { removedCtxValReferenceName() }
    if (testMode.isJs) {
        expectSuccess { removedCtxValReferenceHashCode(); "OK" }
        expectSuccess(false) { removedCtxValReferenceEquals() }
        expectSuccess { removedCtxValReferenceToString(); "OK" }
    } else {
        expectFailure(linkage("Reference to property 'removedCtxVal' can not be evaluated: No property found for symbol '/removedCtxVal'")) { removedCtxValReferenceHashCode() }
        expectFailure(linkage("Reference to property 'removedCtxVal' can not be evaluated: No property found for symbol '/removedCtxVal'")) { removedCtxValReferenceEquals() }
        expectFailure(linkage("Reference to property 'removedCtxVal' can not be evaluated: No property found for symbol '/removedCtxVal'")) { removedCtxValReferenceToString() }
    }
    expectFailure(linkage("Property accessor 'removedCtxVal.<get-removedCtxVal>' can not be called: No property accessor found for symbol '/removedCtxVal.<get-removedCtxVal>'")) { removedCtxValReferenceInvoke() }
    expectFailure(linkage("Property accessor 'removedCtxVal.<get-removedCtxVal>' can not be called: No property accessor found for symbol '/removedCtxVal.<get-removedCtxVal>'")) { removedCtxValReferenceGet() }

    // var
    expectSuccess(true) { createRemovedCtxVarReference() is kotlin.reflect.KMutableProperty0<*> }
    expectSuccess("removedCtxVar") { removedCtxVarReferenceName() }
    if (testMode.isJs) {
        expectSuccess { removedCtxVarReferenceHashCode(); "OK" }
        expectSuccess(false) { removedCtxVarReferenceEquals() }
        expectSuccess { removedCtxVarReferenceToString(); "OK" }
    } else {
        expectFailure(linkage("Reference to property 'removedCtxVar' can not be evaluated: No property found for symbol '/removedCtxVar'")) { removedCtxVarReferenceHashCode() }
        expectFailure(linkage("Reference to property 'removedCtxVar' can not be evaluated: No property found for symbol '/removedCtxVar'")) { removedCtxVarReferenceEquals() }
        expectFailure(linkage("Reference to property 'removedCtxVar' can not be evaluated: No property found for symbol '/removedCtxVar'")) { removedCtxVarReferenceToString() }
    }
    expectFailure(linkage("Property accessor 'removedCtxVar.<get-removedCtxVar>' can not be called: No property accessor found for symbol '/removedCtxVar.<get-removedCtxVar>'")) { removedCtxVarReferenceInvoke() }
    expectFailure(linkage("Property accessor 'removedCtxVar.<get-removedCtxVar>' can not be called: No property accessor found for symbol '/removedCtxVar.<get-removedCtxVar>'")) { removedCtxVarReferenceGet() }
    expectFailure(linkage("Property accessor 'removedCtxVar.<set-removedCtxVar>' can not be called: No property accessor found for symbol '/removedCtxVar.<set-removedCtxVar>'")) { removedCtxVarReferenceSet() }

    // var delegated to a reference to the var with context parameter
    expectSuccess(true) { createRemovedCtxVarDelegateReference() is kotlin.reflect.KMutableProperty0<*> }
    expectSuccess("removedCtxVarDelegate") { removedCtxVarDelegateReferenceName() }
    expectSuccess { removedCtxVarDelegateReferenceHashCode(); "OK" }
    expectSuccess(false) { removedCtxVarDelegateReferenceEquals() }
    expectSuccess { removedCtxVarDelegateReferenceToString(); "OK" }
    expectFailure(linkage("Property accessor 'removedCtxVar.<get-removedCtxVar>' can not be called: No property accessor found for symbol '/removedCtxVar.<get-removedCtxVar>'")) { removedCtxVarDelegateReferenceInvoke() }
    expectFailure(linkage("Property accessor 'removedCtxVar.<get-removedCtxVar>' can not be called: No property accessor found for symbol '/removedCtxVar.<get-removedCtxVar>'")) { removedCtxVarDelegateReferenceGet() }
    expectFailure(linkage("Property accessor 'removedCtxVar.<set-removedCtxVar>' can not be called: No property accessor found for symbol '/removedCtxVar.<set-removedCtxVar>'")) { removedCtxVarDelegateReferenceSet() }

    // changed context parameters
    expectSuccess("funWithAddedCtx") { funWithAddedCtxReferenceName() }
    expectFailure(linkage("Function 'funWithAddedCtx' can not be called: No function found for symbol '/funWithAddedCtx'")) { funWithAddedCtxReferenceInvoke() }

    expectSuccess("funWithRemovedCtx") { funWithRemovedCtxReferenceName() }
    expectFailure(linkage("Function 'funWithRemovedCtx' can not be called: No function found for symbol '/funWithRemovedCtx'")) { funWithRemovedCtxReferenceInvoke() }

    expectFailure(linkage("Function 'funWithCtxTurnedIntoParam' can not be called: No function found for symbol '/funWithCtxTurnedIntoParam'")) { funWithCtxTurnedIntoParamReferenceInvoke() }

    expectFailure(linkage("Function 'funWithParamTurnedIntoCtx' can not be called: No function found for symbol '/funWithParamTurnedIntoCtx'")) { funWithParamTurnedIntoCtxReferenceInvoke() }

    expectSuccess("funWithAllCtxRemoved") { funWithAllCtxRemovedReferenceName() }
    expectFailure(linkage("Function 'funWithAllCtxRemoved' can not be called: No function found for symbol '/funWithAllCtxRemoved'")) { funWithAllCtxRemovedReferenceInvoke() }

    expectSuccess("funWithCtxGained") { funWithCtxGainedReferenceName() }
    expectFailure(linkage("Function 'funWithCtxGained' can not be called: No function found for symbol '/funWithCtxGained'")) { funWithCtxGainedReferenceInvoke() }

    expectFailure(linkage("Function 'funWithSwappedCtx' can not be called: No function found for symbol '/funWithSwappedCtx'")) { funWithSwappedCtxReferenceInvoke() }

    expectFailure(linkage("Function 'extFunMigratedToCtx' can not be called: No function found for symbol '/extFunMigratedToCtx'")) { extFunMigratedToCtxReferenceInvoke() }

    expectSuccess("funWithRenamedCtx") { funWithRenamedCtxReferenceName() }
    expectSuccess("funWithRenamedCtx.v2(ctx, 123)") { funWithRenamedCtxReferenceInvoke() }

    expectSuccess("valWithChangedCtxType") { valWithChangedCtxTypeReferenceName() }
    expectFailure(linkage("Property accessor 'valWithChangedCtxType.<get-valWithChangedCtxType>' can not be called: No property accessor found for symbol '/valWithChangedCtxType.<get-valWithChangedCtxType>'")) { valWithChangedCtxTypeReferenceGet() }

    // extension fun
    expectSuccess("removedCtxExtFun") { removedCtxExtFunReferenceName() }
    expectFailure(linkage("Function 'removedCtxExtFun' can not be called: No function found for symbol '/removedCtxExtFun'")) { removedCtxExtFunReferenceInvoke() }

    // unbound extension fun
    expectSuccess("removedCtxExtFun") { unboundRemovedCtxExtFunReferenceName() }
    expectFailure(linkage("Function 'removedCtxExtFun' can not be called: No function found for symbol '/removedCtxExtFun'")) { unboundRemovedCtxExtFunReferenceInvoke() }

    // removed context parameter type: the context argument expression fails before the reference is created
    expectFailure(linkage("Type operator expression can not be evaluated: Expression uses unlinked class symbol '/RemovedCtxClass'")) { createFunWithUnlinkedCtxParameterReference() }

    // surviving references
    expectSuccess("survivingCtxFun") { survivingCtxFunReferenceName() }
    expectSuccess("survivingCtxFun(ctx, K, 123)") { survivingCtxFunReferenceInvoke() }
    expectSuccess("survivingMemberCtxFun(ctx, 123)") { survivingMemberCtxFunReferenceInvoke() }
    expectSuccess("survivingCtxExtFun(ctx, 5, 123)") { survivingCtxExtFunReferenceInvoke() }
    expectSuccess("survivingCtxExtFun(ctx, 5, 123)") { unboundSurvivingCtxExtFunReferenceInvoke() }
    expectSuccess("survivingCtxVar(ctx, value)") { survivingCtxVarReferenceSetAndGet() }
}
