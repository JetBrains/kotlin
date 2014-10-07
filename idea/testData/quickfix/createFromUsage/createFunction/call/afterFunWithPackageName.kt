// "Create function 'foo' from usage" "true"

package foo

fun test() {
    foo(2, "2")
}

fun foo(i: Int, s: String) {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}