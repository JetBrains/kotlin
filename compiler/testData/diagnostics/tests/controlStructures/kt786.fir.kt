package kt786

//KT-786 Exception on incomplete code with 'when'
fun foo() : Int {
    val d = 2
    var z = 0
    when(d) {
        5, 3 -> z++
        else -> { z = -1000 }
        return z -> 34
    }
}

//test unreachable code
fun fff(): Int {
    var d = 3
    when(d) {
        4 -> 21
        return 2 -> return 47
        bar() -> 45
        444 -> true
    }
    return 34
}

fun bar(): Int = 8