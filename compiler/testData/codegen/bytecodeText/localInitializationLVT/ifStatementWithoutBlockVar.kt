import kotlin.random.Random

fun test(): Char {
    var c: Char
    if (Random.nextBoolean())
        c = '1'
    else
        c = '2'

    return c
}

// 3 ISTORE 0
// 1 LOCALVARIABLE c C L1 L5 0
