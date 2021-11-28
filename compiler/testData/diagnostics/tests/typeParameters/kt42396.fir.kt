// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

interface A
interface B

class Out<out F>
class Inv<F>

fun <F> materializeOutOfAAndB(): Out<F> where F : A, F : B = Out()
fun <F> materializeInvOfAAndB(): Inv<F> where F : A, F : B = Inv()
fun <F> wrapAAndBToOut(x: F): Out<F> where F : A, F : B = Out()

fun main(a: A) {
    val x: Out<A> = materializeOutOfAAndB() // OI: inferred type A is not a subtype of B; `F` is instantiated as `A`, so upper bounds was violated
    val y: Inv<A> = <!TYPE_MISMATCH!>materializeInvOfAAndB()<!> // OI and NI: required B, found A
    val z: Out<A> = wrapAAndBToOut(<!ARGUMENT_TYPE_MISMATCH!>a<!>) // OI and NI: required B, found A
}
