// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM_IR

// WITH_STDLIB

//import kotlin.Self

//@Self
class Foo {
    public val bar = 1

//    fun test(): Self = (this as Self)

    fun box(): String {
//        val isSelf = test().bar
//        println(isSelf)
        return "OK"
    }
}

fun box(): String {
    return Foo().box()
}