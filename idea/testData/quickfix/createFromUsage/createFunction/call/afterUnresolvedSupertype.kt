// "Create function 'foo' from usage" "true"
// ERROR: Unresolved reference: B

class A: B {
    fun foo(): Any {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

fun test() {
    A().foo()
}