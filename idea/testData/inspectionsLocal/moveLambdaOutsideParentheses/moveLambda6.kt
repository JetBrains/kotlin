// IS_APPLICABLE: true
fun foo() {
    bar(3, 2, 1, { it <caret>})
}

fun bar(name1: Int, name2: Int, name3: Int, name4: (Int) -> Int) : Int {
    return name4(name1) + name2 + name3
}
