// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KClass
import kotlin.test.assertEquals

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
    // val ka: Array<KClass<*>>,  // Arrays of class literals are not supported yet in AnnotationDeserializer
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
    // [Double::class, Unit::class, LongArray::class],
    [AnnotationTarget.TYPEALIAS, AnnotationTarget.FIELD],
    [Nested("2"), Nested("3")]
) Unit {}

fun box(): String {
    assertEquals(
        "[@Anno(b=1, c=x, d=3.14, f=-2.72, i=42424242, j=239239239239239, s=42, z=true, " +
                "ba=[-1], ca=[y], da=[-3.14159], fa=[2.7218], ia=[424242], ja=[239239239239], sa=[-43], za=[false, true], " +
                "str=lol, k=class java.lang.Number, k2=class [I, e=EXPRESSION, a=@Nested(value=1), " +
                "stra=[lmao], ea=[TYPEALIAS, FIELD], aa=[@Nested(value=2), @Nested(value=3)])]",
        ::f.returnType.annotations.toString()
    )

    return "OK"
}
