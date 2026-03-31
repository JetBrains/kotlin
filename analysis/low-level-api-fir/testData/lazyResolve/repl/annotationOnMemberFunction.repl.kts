annotation class Anno(val value: String)

class MyClass {
    @Anno("test")
    fun resolve<caret>Me() {}
}
