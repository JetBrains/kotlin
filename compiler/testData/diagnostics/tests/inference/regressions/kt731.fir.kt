// !WITH_NEW_INFERENCE
// !CHECK_TYPE

//KT-731 Missing error from type inference
package a

import checkSubtype

class A<T>(x: T) {
    val p = x
}

fun <T, G> A<T>.foo(x: (T)-> G): G {
    return x(this.p)
}

fun main() {
    val a = A(1)
    val t: String = a.foo({p -> <!ARGUMENT_TYPE_MISMATCH!>p<!>})
    checkSubtype<String>(t)
}
