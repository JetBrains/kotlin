//KT-4403 Wrong "type mismatch" on smart cast with inference

interface A
interface B : A

fun <T> T.f(): T = this

fun test(a: A) {
    if (a !is B) return
    val <!UNUSED_VARIABLE!>c<!> = <!DEBUG_INFO_SMARTCAST!>a<!>.f() // type mismatch
}