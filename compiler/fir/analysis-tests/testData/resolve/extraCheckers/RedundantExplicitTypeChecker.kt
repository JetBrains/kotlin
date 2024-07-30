import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class A

fun annotated() {
    val <!UNUSED_VARIABLE!>x<!>: @A Int /* NOT redundant */ = 1
}

object SomeObj
fun fer() {
    val <!UNUSED_VARIABLE!>x<!>: Any /* NOT redundant */ = SomeObj
}

fun f2(y: String?): String {
    val <!UNUSED_VARIABLE!>f<!>: KClass<*> = (y ?: return "")::class
    return ""
}

object Obj {}

interface IA
interface IB : IA

fun IA.extFun(x: IB) {}

fun testWithExpectedType() {
    val <!UNUSED_VARIABLE!>extFun_AB_A<!>: IA.(IB) -> Unit = IA::extFun
}

interface Point {
    val x: Int
    val y: Int
}

class PointImpl(override val x: Int, override val y: Int) : Point

fun foo() {
    val <!UNUSED_VARIABLE!>s<!>: <!REDUNDANT_EXPLICIT_TYPE!>String<!> = "Hello ${10+1}"
    val <!UNUSED_VARIABLE!>str<!>: String? = ""

    val <!UNUSED_VARIABLE!>o<!>: <!REDUNDANT_EXPLICIT_TYPE!>Obj<!> = Obj

    val <!UNUSED_VARIABLE!>p<!>: Point = PointImpl(1, 2)
    val <!UNUSED_VARIABLE!>a<!>: <!REDUNDANT_EXPLICIT_TYPE!>Boolean<!> = true
    val <!UNUSED_VARIABLE!>i<!>: Int = 2 * 2
    val <!UNUSED_VARIABLE!>l<!>: <!REDUNDANT_EXPLICIT_TYPE!>Long<!> = 1234567890123L
    val <!UNUSED_VARIABLE!>s1<!>: String? = null
    val <!UNUSED_VARIABLE!>sh<!>: Short = 42

    val <!UNUSED_VARIABLE!>integer<!>: <!REDUNDANT_EXPLICIT_TYPE!>Int<!> = 42
    val <!UNUSED_VARIABLE!>piFloat<!>: <!REDUNDANT_EXPLICIT_TYPE!>Float<!> = 3.14f
    val <!UNUSED_VARIABLE!>piDouble<!>: <!REDUNDANT_EXPLICIT_TYPE!>Double<!> = 3.14
    val <!UNUSED_VARIABLE!>charZ<!>: <!REDUNDANT_EXPLICIT_TYPE!>Char<!> = 'z'
    <!CAN_BE_VAL!>var<!> <!UNUSED_VARIABLE!>alpha<!>: <!REDUNDANT_EXPLICIT_TYPE!>Int<!> = 0
}

fun test(boolean: Boolean) {
    val <!UNUSED_VARIABLE!>expectedLong<!>: Long = if (boolean) {
        42
    } else {
        return
    }
}

class My {
    val x: Int = 1
}

val ZERO: Int = 0

fun main() {
    val <!UNUSED_VARIABLE!>id<!>: Id = 11
}

typealias Id = Int
