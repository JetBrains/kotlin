package foo

class A {
    var prop: Int = 0
}

object B {
    var prop: Int = 0
}

val array: Array<Int> = arrayOf(0)

fun <T> id(t: T): T {
    return t
}

fun runMe() {
    val a = A()

    id(a).prop = 10
    id(a).prop += 20
    id(a).prop -= 20
    id(a).prop *= 2
    id(a).prop /= 5
    id(a).prop %= 3

    id(a).prop++
    id(a).prop--
    ++id(a).prop
    --id(a).prop

    B.prop++
    B.prop--
    ++B.prop
    --B.prop

    id(array)[0] = 10
    id(array)[0] += 20
    id(array)[0] -= 20
    id(array)[0] *= 2
    id(array)[0] /= 5
    id(array)[0] %= 3

    id(array)[0]++
    id(array)[0]--
    ++id(array)[0]
    --id(array)[0]

    array[0]++
    array[0]--
    ++array[0]
    --array[0]
}
