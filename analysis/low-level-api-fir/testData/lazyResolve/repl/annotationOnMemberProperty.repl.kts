annotation class Anno(val value: String)

class MyClass {
    @Anno("test")
    val resolve<caret>Me: Int = 42
}
