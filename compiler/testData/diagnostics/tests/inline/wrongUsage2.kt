// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNNECESSARY_SAFE_CALL -UNNECESSARY_NOT_NULL_ASSERTION

public inline fun assertNot(message: String, block: ()-> Boolean) {}

public inline fun assertNot(block: ()-> Boolean) : Unit = assertNot(<!USAGE_IS_NOT_INLINABLE!>block<!>.toString(), block)


public fun <T> callable(action: ()-> T) {

}

public inline fun <T> String.submit(action: ()->T) {
    callable(<!USAGE_IS_NOT_INLINABLE!>action<!>)
}

public inline fun <T> Function1<Int, Int>.submit() {
    <!USAGE_IS_NOT_INLINABLE!>this<!>?.invoke(11)
    <!USAGE_IS_NOT_INLINABLE, USAGE_IS_NOT_INLINABLE!>this<!>!!.invoke(11)
}

public inline fun <T> submit(action: Function1<Int, Int>) {
    <!USAGE_IS_NOT_INLINABLE!>action<!>?.invoke(10)
    <!USAGE_IS_NOT_INLINABLE, USAGE_IS_NOT_INLINABLE!>action<!>!!.invoke(10)
}