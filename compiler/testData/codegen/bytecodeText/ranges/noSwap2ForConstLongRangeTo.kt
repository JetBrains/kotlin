// WITH_RUNTIME

fun test(a: Long) = a in 1L .. 10L

// One DUP2_X2 generated for 'in' operator,
// no DUP2_X2 generated for range on stack.
// 1 DUP2_X2