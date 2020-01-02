// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NULLABLE_INLINE_PARAMETER -CONFLICTING_JVM_DECLARATIONS

inline fun test() {

}

inline fun test2(s : (Int) -> Int) {

}

inline fun test3(noinline s : (Int) -> Int) {

}

inline fun test4(noinline s : Int.() -> Int) {

}

inline fun Function1<Int, Int>?.test5() {

}

inline fun Function1<Int, Int>?.test6() {

}

inline fun test2(s : ((Int) -> Int)?) {

}