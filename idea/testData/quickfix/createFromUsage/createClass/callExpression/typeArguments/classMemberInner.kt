// "Create class 'Foo'" "true"
// ERROR: Type mismatch: inferred type is kotlin.String but V was expected
// ERROR: The integer literal does not conform to the expected type U

class B<T>(val t: T) {

}

class A<T>(val b: B<T>) {
    fun test() = b.<caret>Foo<Int, String>(2, "2")
}