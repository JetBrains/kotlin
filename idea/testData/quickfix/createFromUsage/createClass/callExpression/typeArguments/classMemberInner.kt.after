// "Create class 'Foo'" "true"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>V</td></tr><tr><td>Found:</td><td>kotlin.String</td></tr></table></html>
// ERROR: An integer literal does not conform to the expected type U

class B<T>(val t: T) {
    inner class Foo<U, V>(u: U, v: V) {

    }

}

class A<T>(val b: B<T>) {
    fun test() = b.Foo<Int, String>(2, "2")
}