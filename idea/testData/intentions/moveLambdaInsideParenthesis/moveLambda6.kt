// IS_APPLICABLE: true
fun foo() {
    bar(name1 = 3) <caret>{ it }
}

fun bar(name1: Int, name2: Int->Int) : Int {
    return name2(name1)
}