// "Create class 'Foo'" "false"
// ACTION: Create extension function 'List<T>.Foo'
// ACTION: Rename reference
// ACTION: Convert to run
// ACTION: Convert to with
// ERROR: Unresolved reference: Foo
// WITH_RUNTIME

class A<T>(val items: List<T>) {
    fun test() = items.<caret>Foo<Int, String>(2, "2")
}