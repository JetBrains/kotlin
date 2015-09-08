class Z(<!UNUSED_PARAMETER!>s<!>: (Int) -> Int) {

}

public inline fun test(s : (Int) -> Int) {
    Z(<!USAGE_IS_NOT_INLINABLE!>s<!>)
}