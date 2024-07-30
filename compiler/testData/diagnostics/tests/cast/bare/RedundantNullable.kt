// FIR_IDENTICAL
// WITH_EXTRA_CHECKERS
interface B<T>
class G<T>: B<T>

fun f(b: B<String>?) = b is G?<!REDUNDANT_NULLABLE!>?<!>