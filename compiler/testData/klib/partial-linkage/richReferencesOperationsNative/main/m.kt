import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Reference to function 'removedFun' can not be evaluated: No function found for symbol '/removedFun'")) { removedFunReferenceReturnType() }
    expectFailure(linkage("Reference to constructor 'ClassWithRemovedCtor.<init>' can not be evaluated: No constructor found for symbol '/ClassWithRemovedCtor.<init>'")) { removedCtorReferenceReturnType() }
    expectFailure(linkage("Reference to property 'removedVal' can not be evaluated: No property found for symbol '/removedVal'")) { removedValReferenceReturnType() }
    expectFailure(linkage("Reference to property 'removedVar' can not be evaluated: No property found for symbol '/removedVar'")) { removedVarReferenceReturnType() }
    expectSuccess { removedVarDelegateReferenceReturnType(); "OK" }
}
