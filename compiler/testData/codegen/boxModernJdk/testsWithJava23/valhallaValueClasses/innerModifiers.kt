// LANGUAGE: +ValhallaValueClasses
// ENABLE_JVM_PREVIEW
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: ANDROID
// IGNORE_DEXING
// CHECK_BYTECODE_LISTING

class A {
    @JvmField
    val instance = "OK"
    companion object {
        @JvmField
        val static = "OK"
    }
    class B(val x: Char)
    open class C(val x: Char)
    abstract class D(val x: Char)
    inner class E(val x: Char)
    interface F {
        val x: Char
        companion object {
            @JvmField
            val static = "OK"
        }
    }
    annotation class G(val x: Char)
    enum class H { OK }
}

fun box(): String {
    require(A.B('a').x == 'a')
    require(A.C('a').x == 'a')
    require(object : A.D('a') {}.x == 'a')
    require(A().E('a').x == 'a')
    require(object : A.F { override val x: Char get() = 'a' }.x == 'a')
    require(A().instance == "OK")
    require(A.static == "OK")
    require(A.F.static == "OK")
    return "OK"
}
