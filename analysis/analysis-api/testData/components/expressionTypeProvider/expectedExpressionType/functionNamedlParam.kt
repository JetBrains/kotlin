fun x() {
    toCall(1, z = a<caret>v)
}

fun toCall(x: Int, y: String, z: Boolean): Char = 'a'

