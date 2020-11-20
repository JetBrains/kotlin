// FUNCTION: bar

package test.locals

class Owner {
    fun foo(i: Int) {
        var x = true

        fun bar() {
            baz(i, x)
        }
    }

    fun baz(j: Int, y: Boolean) {}
}