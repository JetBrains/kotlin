// "Make primary constructor parameter 'foo' a property" "true"

class A(foo: String) {
    fun bar() {
        val a = foo<caret>
    }
}