// LANGUAGE: +ValhallaValueClasses
// IGNORE_BACKEND_K1: ANY
// ENABLE_JVM_PREVIEW
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: ANDROID
// IGNORE_DEXING
// CHECK_BYTECODE_LISTING

@JvmInline
value class InnerInline(val x: Int)

value class Valhalla(val x: InnerInline, val y: InnerInline)

@JvmInline
value class OuterInline(val x: Valhalla)

fun box(): String {
    val inner1 = InnerInline(1)
    val inner2 = InnerInline(2)
    require(inner1.x == 1)
    require(inner2.x == 2)

    val valhalla = Valhalla(inner1, inner2)
    require(valhalla.x == inner1)
    require(valhalla.y == inner2)

    val outer = OuterInline(valhalla)
    require(outer.x == valhalla)

    return "OK"
}
