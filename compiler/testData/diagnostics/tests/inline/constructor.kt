class Z(s: (Int) -> Int) {

}

public inline fun test(s : (Int) -> Int) {
    <!INVISIBLE_MEMBER_FROM_INLINE!>Z<!>(<!USAGE_IS_NOT_INLINABLE!>s<!>)
}