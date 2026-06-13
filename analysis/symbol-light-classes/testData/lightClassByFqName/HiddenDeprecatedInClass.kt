// a.A
package a

class A {
    @Deprecated("f", level = DeprecationLevel.HIDDEN)
    fun f() {

    }
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: A.class[f]
