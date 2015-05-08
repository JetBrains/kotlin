// "Create class 'Foo'" "true"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>V</td></tr><tr><td>Found:</td><td>kotlin.String</td></tr></table></html>
// ERROR: An integer literal does not conform to the expected type U

class B<T>(val t: T) {

}

class A<T>(val b: B<T>) {
    fun test() = b.<caret>Foo<Int, String>(2, "2")
}