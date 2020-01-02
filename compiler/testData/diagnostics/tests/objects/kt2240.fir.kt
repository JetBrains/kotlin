package a

//KT-2240 Wrong overload resolution ambiguity when object literal is involved

class A {}

fun <T> A.foo(f : T) {}

val o = object {
    fun <T> foo(f: T) {
        A().foo(f) // Ambiguity here!
    }
}