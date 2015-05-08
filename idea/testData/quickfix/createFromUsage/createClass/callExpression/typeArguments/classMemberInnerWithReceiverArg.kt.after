// "Create class 'Foo'" "true"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>W</td></tr><tr><td>Found:</td><td>kotlin.String</td></tr></table></html>
// ERROR: An integer literal does not conform to the expected type V

class B<T>(val t: T) {
    inner class Foo<U, V, W>(v: V, w: W) {

    }

}

class A<T>(val b: B<T>) {
    fun test() = b.Foo<T, Int, String>(2, "2")
}