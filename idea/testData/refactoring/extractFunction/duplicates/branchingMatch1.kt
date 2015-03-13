// WITH_RUNTIME
// SUGGESTED_NAMES: i, getT
// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter val a: kotlin.Int defined in test
// PARAM_DESCRIPTOR: var b: kotlin.Int defined in test

// SIBLING:
fun test(a: Int): Int {
    var b: Int = 1
    val t = <selection>if (a > 0) {
        b++
        b + a
    }
    else {
        b--
        b - a
    }</selection>
    return t
}

fun foo1() {
    val x = 1
    var y: Int = x
    println(
            if (x > 0) {
                y++
                y + x
            }
            else {
                y--
                y - x
            }
    )
}

fun foo2(x: Int) {
    var p: Int = 1
    var q: Int
    if (x > 0) {
        p++
        q = p + x
    }
    else {
        p--
        q = p - x
    }
    println(q)
}

fun foo3(x: Int): Int {
    var p: Int = 1
    if (x > 0) {
        p++
        return p + x
    }
    else {
        p--
        return p - x
    }
}

fun foo4() {
    val t: (Int) -> (Int) = {
        var n = it
        if (it > 0) {
            n++
            n + it
        }
        else {
            n--
            n - it
        }
    }
    println(t(1))
}

fun foo5(x: Int): Int {
    var p: Int = 1
    if (x > 0) {
        p++
        val t = p + x
    }
    else {
        p--
        val u = p - x
    }
}
