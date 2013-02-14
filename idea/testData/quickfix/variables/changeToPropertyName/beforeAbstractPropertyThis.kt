// "Change '$bar' to 'bar'" "true"
abstract class Bar {
    abstract var bar : String
    fun foo() = "foo" + <caret>this.$bar
}