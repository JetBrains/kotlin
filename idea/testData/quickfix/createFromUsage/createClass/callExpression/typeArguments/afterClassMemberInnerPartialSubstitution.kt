// "Create class 'Foo'" "true"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>U</td></tr><tr><td>Found:</td><td>kotlin.String</td></tr></table></html>

class B<T>(val t: T) {
    inner class Foo<U>(i: Int, u: U) {

    }

}

class A<T>(val b: B<T>) {
    fun test() = b.Foo<String>(2, "2")
}