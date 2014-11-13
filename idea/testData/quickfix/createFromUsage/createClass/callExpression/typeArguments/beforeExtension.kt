// "Create class 'Foo'" "false"
// ACTION: Create function 'Foo'
// ERROR: Unresolved reference: Foo

class A<T>(val items: List<T>) {
    fun test() = items.<caret>Foo<Int, String>(2, "2")
}