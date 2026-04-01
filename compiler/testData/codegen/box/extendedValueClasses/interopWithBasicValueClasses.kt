// LANGUAGE: +ValueClasses
// CHECK_BYTECODE_LISTING
// WORKS_WHEN_VALUE_CLASS

OPTIONAL_JVM_INLINE_ANNOTATION
value class InnerInline(val x: Int)

value class ExtendedVC(val x: InnerInline, val y: InnerInline)

OPTIONAL_JVM_INLINE_ANNOTATION
value class OuterInline(val x: ExtendedVC)

fun box(): String {
    val inner1 = InnerInline(1)
    val inner2 = InnerInline(2)
    require(inner1.x == 1)
    require(inner2.x == 2)

    val extended = ExtendedVC(inner1, inner2)
    require(extended.x == inner1)
    require(extended.y == inner2)

    val outer = OuterInline(extended)
    require(outer.x == extended)

    return "OK"
}
