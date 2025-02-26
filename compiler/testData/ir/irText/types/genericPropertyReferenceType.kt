// FIR_IDENTICAL

import kotlin.reflect.KMutableProperty

class C<T>(var x: T)

var <T> C<T>.y
    get() = x
    set(v) {
        x = v
    }

fun use(p: KMutableProperty<String>) {}

fun test1() {
    use(C("abc")::y)
}

fun test2(a: Any) {
    a as C<String>
    use(a::y)
}
