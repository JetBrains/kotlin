//NO_CHECK_LAMBDA_INLINING
import test.*

class A : Base() {

    override fun method() = "fail method"

    override val prop = "fail property"

    fun test1(): String {
        return call {
            super.method() + super.prop
        }
    }

    fun test2(): String {
        return call {
            call {
                super.method() + super.prop
            }
        }
    }
}

fun box(): String {
    val a = A()
    if (a.test1() != "OK") return "fail 1: ${a.test1()}"
    return a.test2()
}