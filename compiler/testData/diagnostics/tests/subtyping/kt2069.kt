//KT-2069 Cannot call super method when superclass has type parameters
package kt2069

trait T1 {
    fun foo() {}
}

class T : T1 {
    fun bar() {
        super<T1>.foo()
    }

    default object {}
}