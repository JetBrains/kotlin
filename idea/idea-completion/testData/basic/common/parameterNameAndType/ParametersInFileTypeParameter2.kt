// FIR_COMPARISON
package ppp

class X<T1> {
    fun <T2> f5(xxxValue1: T1, xxxValue2: T2){}

    class Nested {
        fun foo(xxx<caret>)
    }
}

// NOTHING_ELSE
