// ISSUE: KT-85698

import kotlin.reflect.KMutableProperty

class C<T>(var x: T)

var <T> C<T>.y
    get() = x
    set(v) {
        x = v
    }

class A {
    var <T> C<T>.z
        get() = x
        set(v) {
            x = v
        }
}

class B {
    class N {
        var <T> C<T>.z
            get() = x
            set(v) {
                x = v
            }
    }
}

class Pair<X, Y>(var x: X, var y: Y)

var <X, Y> Pair<X, Y>.first
    get() = x
    set(v) {
        x = v
    }

fun use(p: KMutableProperty<String>) {}

fun test1() {
    use(C("abc")::y)
    use(Pair("abc", "def")::first)
}

fun test2(a: Any) {
    a as C<String>
    use(a::y)

    a as Pair<String, String>
    use(a::first)
}
