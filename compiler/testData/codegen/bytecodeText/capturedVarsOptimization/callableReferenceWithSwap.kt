private inline fun scan(n: Int, charAt: (Int) -> Char): Int {
    var acc = 0
    var i = 0
    while (i < n) {
        var c = charAt(i).code
        acc += run { c = charAt(i).code; c }
        i++
    }
    return acc
}

fun test(s: String): Int = run { scan(s.length, s::get) }

// 0 NEW kotlin/jvm/internal/Ref\$IntRef
// 0 java/lang/Character\.valueOf
// 2 java/lang/Character\.charValue
