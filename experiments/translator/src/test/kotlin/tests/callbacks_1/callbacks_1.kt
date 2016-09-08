
external fun apply_c(arg: Int, x: (Int)-> Int): Int

fun callback_1_inc(i: Int): Int {
    return i + 1
}

fun callback_1_dec(i: Int): Int {
    return i - 1
}

fun callback_1_apply(arg: Int, x: (Int) -> Int): Int {
    return x(arg)
}

fun compact_test(x: Int): Int {
    return callback_1_apply(callback_1_apply(callback_1_apply(x, ::callback_1_inc), ::callback_1_inc), ::callback_1_dec)
}

fun external_test(x: Int): Int {
    return apply_c(apply_c(apply_c(x, ::callback_1_dec), ::callback_1_dec), ::callback_1_inc)
}

fun mixed_test(x: Int): Int {
    return callback_1_apply(apply_c(callback_1_apply(apply_c(callback_1_apply(x, ::callback_1_inc), ::callback_1_inc), ::callback_1_inc), ::callback_1_dec), ::callback_1_inc)
}