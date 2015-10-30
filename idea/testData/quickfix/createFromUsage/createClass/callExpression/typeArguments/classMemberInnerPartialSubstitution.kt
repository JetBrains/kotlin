// "Create class 'Foo'" "true"
// ERROR: Type mismatch: inferred type is kotlin.String but U was expected

class B<T>(val t: T) {

}

class A<T>(val b: B<T>) {
    fun test() = b.<caret>Foo<String>(2, "2")
}