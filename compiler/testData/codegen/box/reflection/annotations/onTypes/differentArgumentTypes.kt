// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT
package test

import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue

annotation class Nested(val value: String)

@Target(AnnotationTarget.TYPE)
annotation class Anno(
    val b: Byte,
    val c: Char,
    val d: Double,
    val f: Float,
    val i: Int,
    val j: Long,
    val s: Short,
    val z: Boolean,
    val ba: ByteArray,
    val ca: CharArray,
    val da: DoubleArray,
    val fa: FloatArray,
    val ia: IntArray,
    val ja: LongArray,
    val sa: ShortArray,
    val za: BooleanArray,
    val str: String,
    val k: KClass<*>,
    val k2: KClass<*>,
    val e: AnnotationTarget,
    val a: Nested,
    val stra: Array<String>,
    val ka: Array<KClass<*>>,
    val ea: Array<AnnotationTarget>,
    val aa: Array<Nested>
)

fun f(): @Anno(
    1.toByte(),
    'x',
    3.14,
    -2.72f,
    42424242,
    239239239239239L,
    42.toShort(),
    true,
    [(-1).toByte()],
    ['y'],
    [-3.14159],
    [2.7218f],
    [424242],
    [239239239239L],
    [(-43).toShort()],
    [false, true],
    "lol",
    Number::class,
    IntArray::class,
    AnnotationTarget.EXPRESSION,
    Nested("1"),
    ["lmao"],
    [Double::class, Unit::class, LongArray::class, Array<String>::class],
    [AnnotationTarget.TYPEALIAS, AnnotationTarget.FIELD],
    [Nested("2"), Nested("3")]
) Unit {}

fun box(): String {
    val anno = ::f.returnType.annotations.single() as Anno
    assertEquals(
        "@test.Anno(b=1, c=x, d=3.14, f=-2.72, i=42424242, j=239239239239239, s=42, z=true, " +
                "ba=[-1], ca=[y], da=[-3.14159], fa=[2.7218], ia=[424242], ja=[239239239239], sa=[-43], za=[false, true], " +
                "str=lol, k=class java.lang.Number, k2=class [I, e=EXPRESSION, a=@test.Nested(value=1), stra=[lmao], " +
                "ka=[class java.lang.Double, class kotlin.Unit, class [J, class [Ljava.lang.String;], " +
                "ea=[TYPEALIAS, FIELD], aa=[@test.Nested(value=2), @test.Nested(value=3)])",
        anno.toString()
    )

    // Check that array instances have correct types at runtime and not just Object[].
    assertTrue(anno.ba is ByteArray)
    assertTrue(anno.ca is CharArray)
    assertTrue(anno.da is DoubleArray)
    assertTrue(anno.fa is FloatArray)
    assertTrue(anno.ia is IntArray)
    assertTrue(anno.ja is LongArray)
    assertTrue(anno.sa is ShortArray)
    assertTrue(anno.za is BooleanArray)
    val stra = anno.stra
    assertTrue(stra is Array<*> && stra.isArrayOf<String>())
    val ka = anno.ka
    assertTrue(ka is Array<*> && ka.isArrayOf<KClass<*>>())
    val ea = anno.ea
    assertTrue(ea is Array<*> && ea.isArrayOf<AnnotationTarget>())
    val aa = anno.aa
    assertTrue(aa is Array<*> && aa.isArrayOf<Nested>())

    return "OK"
}
