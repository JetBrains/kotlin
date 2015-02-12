// "Create function 'foo'" "true"
class A {
    fun test() {
        foo()
    }

    private fun foo() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}