class TestClass<T>
private fun test(): () -> TestClass<out Any> = {
    class Local
    TestClass<Local>()
}