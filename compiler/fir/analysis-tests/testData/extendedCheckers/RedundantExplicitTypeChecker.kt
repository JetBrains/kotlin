import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class A

fun annotated() {
    val x: @A Int /* NOT redundant */ = 1
}

object SomeObj
fun fer() {
    val x: Any /* NOT redundant */ = SomeObj
}

fun f2(y: String?): String {
    val f: KClass<*> = (y ?: return "")::class
    return ""
}

object Obj {}

interface IA
interface IB : IA

fun IA.extFun(x: IB) {}

fun testWithExpectedType() {
    val extFun_AB_A: IA.(IB) -> Unit = IA::extFun
}

interface Point {
    val x: Int
    val y: Int
}

class PointImpl(override val x: Int, override val y: Int) : Point

fun foo() {
    val s: <!REDUNDANT_EXPLICIT_TYPE!>String<!> = "Hello ${10+1}"
    val str: String? = ""

    val o: <!REDUNDANT_EXPLICIT_TYPE!>Obj<!> = Obj

    val p: Point = PointImpl(1, 2)
    val a: <!REDUNDANT_EXPLICIT_TYPE!>Boolean<!> = true
    val i: Int = 2 * 2
    val l: <!REDUNDANT_EXPLICIT_TYPE!>Long<!> = 1234567890123L
    val s: String? = null
    val sh: Short = 42

    val integer: <!REDUNDANT_EXPLICIT_TYPE!>Int<!> = 42
    val piFloat: <!REDUNDANT_EXPLICIT_TYPE!>Float<!> = 3.14f
    val piDouble: <!REDUNDANT_EXPLICIT_TYPE!>Double<!> = 3.14
    val charZ: <!REDUNDANT_EXPLICIT_TYPE!>Char<!> = 'z'
    <!CAN_BE_VAL!>var<!> alpha: <!REDUNDANT_EXPLICIT_TYPE!>Int<!> = 0
}

fun test(boolean: Boolean) {
    val expectedLong: Long = if (boolean) {
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
    val id: Id = 11
}

typealias Id = Int