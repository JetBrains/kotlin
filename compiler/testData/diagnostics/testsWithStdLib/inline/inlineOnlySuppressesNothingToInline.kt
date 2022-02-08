// FIR_IDENTICAL
// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE -UNUSED_PARAMETER -NULLABLE_INLINE_PARAMETER

@kotlin.internal.InlineOnly
inline fun test() {

}

@kotlin.internal.InlineOnly
inline fun test3(noinline s : (Int) -> Int) {

}

@kotlin.internal.InlineOnly
inline fun test4(noinline s : Int.() -> Int) {

}

@kotlin.internal.InlineOnly
inline fun Function1<Int, Int>?.test5() {

}

@kotlin.internal.InlineOnly
inline fun Function1<Int, Int>?.test6() {

}

@kotlin.internal.InlineOnly
inline fun test2(s : ((Int) -> Int)?) {

}
