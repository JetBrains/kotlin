// IGNORE_BACKEND: JVM_IR
interface Test {
    fun test(): String
}

open class Base(val test: Test)

open class Outer(val x: String) {
    open inner class Inner

    inner class JavacBug : Base(
            object : Outer.Inner(), Test {
                override fun test() = x
            }
    )
}

fun box() = Outer("OK").JavacBug().test.test()