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

<!NOTHING_TO_INLINE!>inline<!> fun baz1(<!NULLABLE_INLINE_PARAMETER!>a: ((Any) -> Unit)?<!>): Any? = a
<!NOTHING_TO_INLINE!>inline<!> fun ((Any) -> Unit)?.baz2(): Any? = this

inline fun qux1(c: (Any) -> Unit) = baz1(<!USAGE_IS_NOT_INLINABLE!>c<!>)
inline fun qux2(c: (Any) -> Unit) = <!USAGE_IS_NOT_INLINABLE!>c<!>.baz2()

typealias TA = (Any) -> Unit
inline fun quxx1(a: TA): Any? = a("")
<!NOTHING_TO_INLINE!>inline<!> fun TA.quxx2(): Any? = this("")

inline fun quxxx1(c: (Any) -> Unit) = quxx1(c)
inline fun quxxx2(c: (Any) -> Unit) = <!USAGE_IS_NOT_INLINABLE!>c<!>.quxx2()

typealias TA2 = ((Any) -> Unit)?
<!NOTHING_TO_INLINE!>inline<!> fun quxxx1(<!NULLABLE_INLINE_PARAMETER!>a: TA2<!>): Any? = a?.invoke("")
<!NOTHING_TO_INLINE!>inline<!> fun TA2.quxxx2(): Any? = this?.invoke("")

inline fun quxxxx1(c: (Any) -> Unit) = quxxx1(c)
inline fun quxxxx2(c: (Any) -> Unit) = <!USAGE_IS_NOT_INLINABLE!>c<!>.quxxx2()
