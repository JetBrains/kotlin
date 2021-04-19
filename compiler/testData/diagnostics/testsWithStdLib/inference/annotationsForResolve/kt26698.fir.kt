// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE

open class Base()
class CX : Base()
class CY : Base()

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <@kotlin.internal.OnlyInputTypes T> foo(a: T, b: T) {}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <@kotlin.internal.OnlyInputTypes T : Any> fooA(a: T, b: T) {}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <@kotlin.internal.OnlyInputTypes T : Base> fooB(a: T, b: T) {}


fun usage(x: CX, y: CY) {
    foo(x, y) // expected err, got err
    fooA(x, y) // expected err, got ok
    fooB(x, y) // expected err, got ok
}
