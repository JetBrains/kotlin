// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -CONFLICTING_JVM_DECLARATIONS

public inline fun <T> Function1<Int, Int>?.submit(action: ()->T) {
    this?.invoke(11)
}

public inline fun <T> submit(action: Function1<Int, Int>?, s: () -> Int) {
    action?.invoke(10)
}

public inline fun <T> submitNoInline(noinline action: Function1<Int, Int>?, s: () -> Int) {
    action?.invoke(10)
}

public inline fun <T> Function1<Int, Int>?.submit() {

}

public inline fun <T> submit(action: Function1<Int, Int>?) {

}
