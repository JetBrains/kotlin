// FIR_IDENTICAL
// ISSUE: KT-50020
import kotlin.reflect.KFunction0

<!NOTHING_TO_INLINE!>inline<!> fun foo1(a: Any?): Any? = a
<!NOTHING_TO_INLINE!>inline<!> fun Any?.foo2(): Any? = this

inline fun bar1(c: (Any) -> Unit) = foo1(<!USAGE_IS_NOT_INLINABLE!>c<!>)
inline fun bar2(c: (Any) -> Unit) = <!USAGE_IS_NOT_INLINABLE!>c<!>.foo2()

<!NOTHING_TO_INLINE!>inline<!> fun kfoo1(a: KFunction0<String>): KFunction0<String> = a
<!NOTHING_TO_INLINE!>inline<!> fun KFunction0<String>.kfoo2(): KFunction0<String> = this

<!NOTHING_TO_INLINE!>inline<!> fun bar1(c: KFunction0<String>) = kfoo1(c)
<!NOTHING_TO_INLINE!>inline<!> fun bar2(c: KFunction0<String>) = c.kfoo2()
