package a

//KT-2240 Wrong overload resolution ambiguity when object literal is involved

class A {}

fun A.foo<T>(<!UNUSED_PARAMETER!>f<!> : T) {}

val o = object {
    fun foo<T>(f: T) {
        A().foo(f) // Ambiguity here!
    }
}