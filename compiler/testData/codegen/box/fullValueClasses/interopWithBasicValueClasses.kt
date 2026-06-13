// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_LISTING
// WORKS_WHEN_VALUE_CLASS

OPTIONAL_JVM_INLINE_ANNOTATION
value class InnerInline(val x: Int) {
    fun f(z: InnerInline) {}
    fun g(t: FullValue) {}
    fun h(t1: OuterInline) {}
}

value class FullValue(val x: InnerInline, val y: InnerInline) {
    fun f(z: InnerInline) {}
    fun g(t: FullValue) {}
    fun h(t1: OuterInline) {}
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class OuterInline(val x: FullValue) {
    fun f(z: InnerInline) {}
    fun g(t: FullValue) {}
    fun h(t1: OuterInline) {}
}

fun box(): String {
    val inner1 = InnerInline(1)
    val inner2 = InnerInline(2)
    require(inner1.x == 1)
    require(inner2.x == 2)

    val full = FullValue(inner1, inner2)
    require(full.x == inner1)
    require(full.y == inner2)

    val outer = OuterInline(full)
    require(outer.x == full)

    return "OK"
}
