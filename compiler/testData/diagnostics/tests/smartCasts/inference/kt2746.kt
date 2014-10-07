//KT-2746 Do.smartcasts in inference

class C<T>(t :T)

fun test1(a: Any) {
    if (a is String) {
        val <!UNUSED_VARIABLE!>c<!>: C<String> = C(<!DEBUG_INFO_SMARTCAST!>a<!>)
    }
}


fun f<T>(t :T): C<T> = C(t)

fun test2(a: Any) {
    if (a is String) {
        val <!UNUSED_VARIABLE!>c1<!>: C<String> = f(<!DEBUG_INFO_SMARTCAST!>a<!>)
    }
}
