// FIR_IDENTICAL
import kotlin.reflect.KFunction0

fun main() {
    class A

    class B {
        <!EXPOSED_PROPERTY_TYPE!>val x = ::A<!>
        <!EXPOSED_PROPERTY_TYPE!>val f: KFunction0<A> = x<!>
    }
}
