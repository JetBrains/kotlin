// "Create class 'Foo'" "true"
// ERROR: Unresolved reference: Foo

class A<T>(val b: B<T>) {
    fun test() = b.<caret>Foo<String>(2, "2")
}