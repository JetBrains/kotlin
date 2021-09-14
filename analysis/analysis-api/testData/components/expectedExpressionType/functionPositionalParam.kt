fun x() {
    toCall(1, a<caret>v, true)
}

fun toCall(x: Int, y: String, z: Boolean): Char = 'a'

