// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE -UNUSED_PARAMETER -NULLABLE_INLINE_PARAMETER

<!HIDDEN!>@kotlin.internal.InlineOnly<!>
inline fun test() {

}

<!HIDDEN!>@kotlin.internal.InlineOnly<!>
inline fun test3(noinline s : (Int) -> Int) {

}

<!HIDDEN!>@kotlin.internal.InlineOnly<!>
inline fun test4(noinline s : Int.() -> Int) {

}

<!HIDDEN!>@kotlin.internal.InlineOnly<!>
inline fun Function1<Int, Int>?.test5() {

}

<!HIDDEN!>@kotlin.internal.InlineOnly<!>
inline fun Function1<Int, Int>?.test6() {

}

<!HIDDEN!>@kotlin.internal.InlineOnly<!>
inline fun test2(s : ((Int) -> Int)?) {

}
