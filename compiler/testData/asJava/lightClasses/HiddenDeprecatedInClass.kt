// a.A
package a

class A {
    @Deprecated("f", level = DeprecationLevel.HIDDEN)
    fun f() {

    }
}

// LAZINESS:NoLaziness