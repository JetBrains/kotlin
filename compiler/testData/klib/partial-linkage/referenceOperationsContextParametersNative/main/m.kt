import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Reference to function 'removedCtxFun' can not be evaluated: No function found for symbol '/removedCtxFun'")) { removedCtxFunReferenceReturnType() }
    expectFailure(linkage("Reference to property 'removedCtxVal' can not be evaluated: No property found for symbol '/removedCtxVal'")) { removedCtxValReferenceReturnType() }
    expectFailure(linkage("Reference to property 'removedCtxVar' can not be evaluated: No property found for symbol '/removedCtxVar'")) { removedCtxVarReferenceReturnType() }
    expectSuccess { removedCtxVarDelegateReferenceReturnType(); "OK" }
    expectSuccess { survivingCtxFunReferenceReturnType(); "OK" }
}
