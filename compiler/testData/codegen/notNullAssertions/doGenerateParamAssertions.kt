import test.doGenerateParamAssertions as C

class A : C() {
    override fun bar(s: String) {
    }
}

fun box() = C.foo(A())
