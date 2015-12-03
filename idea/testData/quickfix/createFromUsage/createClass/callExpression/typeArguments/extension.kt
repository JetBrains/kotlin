// "Create class 'Foo'" "false"
// ACTION: Create extension function 'Foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: Foo

class A<T>(val items: List<T>) {
    fun test() = items.<caret>Foo<Int, String>(2, "2")
}