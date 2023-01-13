// c.b.a.Foo$bar$A$B
package c.b.a

class Foo {
    fun bar() {
        class `A$B` {
            inner class `C$D`

            inner class `$$$$$$$` {
                inner class `G$G$`
            }
        }
    }
}

// FIR_COMPARISON