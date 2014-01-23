// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NULLABLE_INLINE_PARAMETER

<!NOTHING_TO_INLINE!>inline fun test()<!> {

}

inline fun test2(s : (Int) -> Int) {

}

<!NOTHING_TO_INLINE!>inline fun test3(noinline s : (Int) -> Int)<!> {

}

<!NOTHING_TO_INLINE!>inline fun test4(noinline s : Int.() -> Int)<!> {

}

<!NOTHING_TO_INLINE!>inline fun Function1<Int, Int>?.test5()<!> {

}

<!NOTHING_TO_INLINE!>inline fun Function1<Int, Int>?.test6()<!> {

}

<!NOTHING_TO_INLINE!>inline fun test2(s : ((Int) -> Int)?)<!> {

}