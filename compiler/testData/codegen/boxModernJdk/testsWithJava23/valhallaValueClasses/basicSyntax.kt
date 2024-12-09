// LANGUAGE: +ValhallaValueClasses
// IGNORE_BACKEND_K1: ANY
// ENABLE_JVM_PREVIEW
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: ANDROID
// IGNORE_DEXING
// CHECK_BYTECODE_LISTING



value class A1(val x: Int)
value class A2(val x: Int, val y: Int)
data class A1_(val x: Int)
data class A2_(val x: Int, val y: Int)

fun box(): String {
    singleField()
    multipleField()
    return "OK"
}

private fun singleField() {
    val a = A1(2)
    val a_ = A1_(2)
    require(a == A1(2)) { a.toString() }
    require(a.x == a_.x) { a.x.toString() }
    require(a.toString() == a_.toString().replace("_", "")) { a.toString() }
    require(a.hashCode() == a_.hashCode()) { "${a.hashCode()} ${A1(2).hashCode()} ${a_.hashCode()}" }
}

private fun multipleField() {
    val a = A2(2, 3)
    val a_ = A2_(2, 3)
    require(a == A2(2, 3)) { a.toString() }
    require(a.x == a_.x) { a.x.toString() }
    require(a.y == a_.y) { a.y.toString() }
    require(a.toString() == a_.toString().replace("_", "")) { a.toString() }
    require(a.hashCode() == a_.hashCode()) { "${a.hashCode()} ${a_.hashCode()}" }
}
