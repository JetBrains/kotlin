import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class A

fun annotated() {
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>x<!>: @A Int /* NOT redundant */ = 1<!>
}

object SomeObj
fun fer() {
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>x<!>: Any /* NOT redundant */ = SomeObj<!>
}

fun f2(y: String?): String {
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>f<!>: KClass<*> = (y ?: return "")::class<!>
    return ""
}

object Obj {}

interface IA
interface IB : IA

fun IA.extFun(x: IB) {}

fun testWithExpectedType() {
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>extFun_AB_A<!>: IA.(IB) -> Unit = IA::extFun<!>
}

interface Point {
    val x: Int
    val y: Int
}

class PointImpl(override val x: Int, override val y: Int) : Point

fun foo() {
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>s<!>: <!REDUNDANT_EXPLICIT_TYPE!>String<!> = "Hello ${10+1}"<!>
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>str<!>: String? = ""<!>

    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>o<!>: <!REDUNDANT_EXPLICIT_TYPE!>Obj<!> = Obj<!>

    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>p<!>: Point = PointImpl(1, 2)<!>
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>a<!>: <!REDUNDANT_EXPLICIT_TYPE!>Boolean<!> = true<!>
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>i<!>: Int = 2 * 2<!>
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>l<!>: <!REDUNDANT_EXPLICIT_TYPE!>Long<!> = 1234567890123L<!>
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>s<!>: String? = null<!>
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>sh<!>: Short = 42<!>

    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>integer<!>: <!REDUNDANT_EXPLICIT_TYPE!>Int<!> = 42<!>
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>piFloat<!>: <!REDUNDANT_EXPLICIT_TYPE!>Float<!> = 3.14f<!>
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>piDouble<!>: <!REDUNDANT_EXPLICIT_TYPE!>Double<!> = 3.14<!>
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>charZ<!>: <!REDUNDANT_EXPLICIT_TYPE!>Char<!> = 'z'<!>
    <!UNUSED_VARIABLE{LT}!><!CAN_BE_VAL!>var<!> <!UNUSED_VARIABLE{PSI}!>alpha<!>: <!REDUNDANT_EXPLICIT_TYPE!>Int<!> = 0<!>
}

fun test(boolean: Boolean) {
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>expectedLong<!>: Long = if (boolean) {
        42
    } else {
        return
    }<!>
}

class My {
    val x: Int = 1
}

val ZERO: Int = 0

fun main() {
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>id<!>: Id = 11<!>
}

typealias Id = Int
