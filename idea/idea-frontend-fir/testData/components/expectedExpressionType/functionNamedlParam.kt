// FIX_ME: should work on non fully resolved calls

fun x() {
    toCall(1, z = a<caret>v)
}

fun toCall(x: Int, y: String, z: Boolean): Char = 'a'

