// !DIAGNOSTICS: -UNUSED_VARIABLE

trait A
trait B

trait C: A, B
trait D: A, B
trait E: A, B

fun foo(c: C?, d: D?, e: E?) {
    val a: A? = <!TYPE_MISMATCH!>c ?: d<!> ?: e

    val b: B? = if (false) <!TYPE_MISMATCH!>if (true) c else d<!> else e

    //outer elvis operator and if-expression have error types
}