// "Create function 'foo' from usage" "true"

class A {
    class B {
        fun test(): Int {
            return foo(2, "2")
        }

        fun foo(i: Int, s: String): Int {
            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}