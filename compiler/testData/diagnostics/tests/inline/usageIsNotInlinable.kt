// ISSUE: KT-50020

<!NOTHING_TO_INLINE!>inline<!> fun foo1(a: Any?): Any? = a
<!NOTHING_TO_INLINE!>inline<!> fun Any?.foo2(): Any? = this

inline fun bar1(c: (Any) -> Unit) = foo1(<!USAGE_IS_NOT_INLINABLE!>c<!>)
inline fun bar2(c: (Any) -> Unit) = <!USAGE_IS_NOT_INLINABLE!>c<!>.foo2()