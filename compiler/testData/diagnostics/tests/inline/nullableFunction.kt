// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -CONFLICTING_JVM_DECLARATIONS

public inline fun <T> Function1<Int, Int>?.submit(action: ()->T) {
    this?.invoke(11)
}

public inline fun <T> submit(<!NULLABLE_INLINE_PARAMETER!>action: Function1<Int, Int>?<!>, s: () -> Int) {
    action?.invoke(10)
}

public inline fun <T> submitNoInline(noinline action: Function1<Int, Int>?, s: () -> Int) {
    action?.invoke(10)
}

public <!NOTHING_TO_INLINE!>inline<!> fun <T> Function1<Int, Int>?.submit() {

}

public <!NOTHING_TO_INLINE!>inline<!> fun <T> submit(<!NULLABLE_INLINE_PARAMETER!>action: Function1<Int, Int>?<!>) {

}