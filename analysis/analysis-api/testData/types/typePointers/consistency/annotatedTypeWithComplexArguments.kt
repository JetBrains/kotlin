// WITH_STDLIB
import kotlin.reflect.KClass

const val myIntConst = 5

class A

enum class E { A, B }

@Target(AnnotationTarget.TYPE)
annotation class Nested(val i: Int, val klass: @SomeTypeAnnotation KClass<*>, val enums: Array<E>, vararg val x: @SomeTypeAnnotation KClass<*>)

@Target(AnnotationTarget.TYPE)
annotation class Complex(
    val s: String,
    val i: Int,
    val l: Long,
    val b: Boolean,
    val c: Char,
    val d: Double,
    val e: E,
    val k: KClass<*>,
    val uByte: UByte,
    val strings: Array<String>,
    val enums: Array<E>,
    val classes: Array<KClass<*>>,
    val primitiveArray: BooleanArray,
    val nested: Nested,
    vararg val x: Int
)

@Target(AnnotationTarget.TYPE)
annotation class SomeTypeAnnotation

fun <T> test(
    value: <expr>@Complex(
    s = ("s" + "a"),
    i = myIntConst,
    l = 42L,
    b = true,
    c = 'c',
    d = 1.5,
    e = E.B,
    k = List::class,
    uByte = 4u,
    strings = (["a", "b"]),
    enums = arrayOf(E.A, E.B),
    classes = [Int::class, Array<String>::class],
    booleanArrayOf(true, (false), false),
    nested = (Nested(7, List::class, arrayOf(E.A, E.B), *arrayOf(String::class), A::class)),
    (1),
    2,
    3,
    *intArrayOf(1, 2, 3)
) List<String></expr>) {
}
