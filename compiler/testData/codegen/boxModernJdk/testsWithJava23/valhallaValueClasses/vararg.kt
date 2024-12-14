// LANGUAGE: +ValhallaValueClasses
// IGNORE_BACKEND_K1: ANY
// ENABLE_JVM_PREVIEW
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: ANDROID
// IGNORE_DEXING
// CHECK_BYTECODE_LISTING

value class Point(val x: Int, val y: Int)

fun f(vararg points: Point) = points.asList().toString()

fun box(): String {
    require(f(Point(1, 2), Point(3, 4)) == "[Point(x=1, y=2), Point(x=3, y=4)]")
    return "OK"
}
