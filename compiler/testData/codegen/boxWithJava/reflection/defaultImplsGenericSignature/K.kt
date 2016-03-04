class B<T>(val value: T)

interface A<T, Y : B<T>> {

    fun <T, L> test1(p: T, z: L): T {
        return p
    }

    fun <L> test2(p: L): A<T, Y> {
        return this
    }
}


class X<T, Y : B<T>>(val p1: T, val p2: Y) : A<T, Y> {

}

fun box(): String {
    val test1 = J.test1()
    if (test1 != 1) return "fail 1: $test1 != 1"

    val test2: X<String, B<String>> = J.test2() as X<String, B<String>>

    return test2.p1 + test2.p2.value
}