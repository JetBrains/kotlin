package wrong

class ClassWithInnerLambda {
    fun test(a: () -> Unit) = a
    fun other() {
        test({})
    }
}