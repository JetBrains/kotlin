// FIX_ME: should work on non fully resolved calls

fun x() {
    toCall(a<caret>b, 12)
}

fun <T> toCall(x: T, y: T): Char = 'a'

