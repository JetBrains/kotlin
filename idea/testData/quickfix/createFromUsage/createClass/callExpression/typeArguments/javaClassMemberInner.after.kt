// "Create class 'Foo'" "true"
// ERROR: Unresolved reference: Foo

class A<T>(val b: B<T>) {
    fun test() = b.Foo<Int, String>(2, "2")
}

