// WITH_RUNTIME

fun testPrimitiveArray(ints: IntArray) =
        10 in ints.indices

// We currently fail to optimize this method because of DUP_X1 instruction generated for range check.
// TODO either don't generate DUP_X1/DUP2_X2 instructions for range checks (extra local variable + extra instructions),
// or support DUPn_Xm instructions in PopBackwardPropagationTransformer
// - 0 DUP
// - 0 POP