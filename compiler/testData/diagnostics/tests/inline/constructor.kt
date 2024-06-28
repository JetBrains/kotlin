// FIR_IDENTICAL
class Z(s: (Int) -> Int) {

}

public inline fun test(s : (Int) -> Int) {
    Z(<!USAGE_IS_NOT_INLINABLE!>s<!>)
}
