import abitestutils.abiTest

fun box() = abiTest {
    /************************************************************/
    /***** Extracted from 'richReferencesOperationsNative': *****/
    /************************************************************/

    expectFailure(linkage("Reference to function 'removedInlineFun' can not be evaluated: No function found for symbol '/removedInlineFun'")) { removedInlineFunReferenceReturnType() }
    expectFailure(linkage("Reference to property 'removedInlineVal' can not be evaluated: No property found for symbol '/removedInlineVal'")) { removedInlineValReferenceReturnType() }
    expectFailure(linkage("Reference to property 'removedInlineVar' can not be evaluated: No property found for symbol '/removedInlineVar'")) { removedInlineVarReferenceReturnType() }
}
