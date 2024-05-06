// FIR_IDENTICAL
<!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> enum class Test

open class Foo {
    protected enum class Test1
    private enum class Test2
    internal enum class Test3
}