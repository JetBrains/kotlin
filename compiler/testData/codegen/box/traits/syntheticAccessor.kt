// IGNORE_BACKEND_FIR: JVM_IR
var result = "fail"

interface B {

    private fun test() {
        result = "OK"
    }

    class Z {
        fun ztest(b: B) {
            b.test()
        }
    }
}

class C : B

fun box(): String {
    B.Z().ztest(C())
    return result
}
