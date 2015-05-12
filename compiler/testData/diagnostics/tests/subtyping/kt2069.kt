//KT-2069 Cannot call super method when superclass has type parameters
package kt2069

interface T1 {
    fun foo() {}
}

class T : T1 {
    fun bar() {
        super<T1>.foo()
    }

    companion object {}
}