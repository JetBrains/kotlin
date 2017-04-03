interface A<T>
class B : A<String> {}

fun <T, U : A<T>> test1(a: U) {
    a is B
}

fun <T, S : A<T>, U : S> test2(a: U) {
    a is B
}

fun <T : A<String>, V : A<Int>> test3(a: T, b: V) {
    a is B
    b is <!INCOMPATIBLE_TYPES!>B<!>
}

fun <T, V : A<out T>> test4(a: T, b: V) {
    a is B
    b is B
}

interface Out<out T>
class OutNothing : Out<Nothing>

fun <T, S : Out<T>> test5(a: S) {
    a is OutNothing
}

interface In<in T>
class InNothing : In<Nothing>

fun <T, S : In<T>> test6(a: S) {
    a is InNothing
}