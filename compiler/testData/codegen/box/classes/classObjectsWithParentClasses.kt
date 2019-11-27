// IGNORE_BACKEND_FIR: JVM_IR
open class Test {
    companion object {
        fun testStatic(ic: InnerClass): NotInnerClass = NotInnerClass(ic.value)
    }

    fun test(): InnerClass = InnerClass(150)

    inner open class InnerClass(val value: Int)
    open class NotInnerClass(val value: Int)
}

fun box() = if (Test.testStatic(Test().test()).value == 150) "OK" else "FAIL"