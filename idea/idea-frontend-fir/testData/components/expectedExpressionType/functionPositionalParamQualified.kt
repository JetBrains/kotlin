fun x() {
    toCall(1, x.a<caret>v, true)
}

fun toCall(x: Int, y: String, z: Boolean): Char = 'a'

// EXPECTED_TYPE: kotlin/String