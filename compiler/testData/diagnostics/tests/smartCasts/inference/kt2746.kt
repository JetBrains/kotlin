//KT-2746 Do autocasts in inference
import java.util.HashMap

class C<T>(t :T)

fun test1(a: Any) {
    if (a is String) {
        val <!UNUSED_VARIABLE!>c<!>: C<String> = C(a)
    }
}


fun f<T>(t :T): C<T> = C(t)

fun test2(a: Any) {
    if (a is String) {
        val <!UNUSED_VARIABLE!>c1<!>: C<String> = f(a)
    }
}
