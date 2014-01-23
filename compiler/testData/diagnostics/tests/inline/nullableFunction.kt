// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

public inline fun <T> <!NULLABLE_INLINE_PARAMETER!>Function1<Int, Int>?<!>.submit(action: ()->T) {
    this?.invoke(11)
}

public inline fun <T> submit(<!NULLABLE_INLINE_PARAMETER!>action: Function1<Int, Int>?<!>, s: () -> Int) {
    action?.invoke(10)
}

public inline fun <T> submitNoInline(noinline action: Function1<Int, Int>?, s: () -> Int) {
    action?.invoke(10)
}

<!NOTHING_TO_INLINE!>public inline fun <T> <!NULLABLE_INLINE_PARAMETER!>Function1<Int, Int>?<!>.submit()<!> {

}

<!NOTHING_TO_INLINE!>public inline fun <T> submit(<!NULLABLE_INLINE_PARAMETER!>action: Function1<Int, Int>?<!>)<!> {

}