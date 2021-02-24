class F
class TestClass<V, K>
private fun test(): () -> TestClass<F, out Any> = {
    class Local
    TestClass<F, Local>()
}