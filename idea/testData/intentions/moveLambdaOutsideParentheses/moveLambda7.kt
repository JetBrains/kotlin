// IS_APPLICABLE: true
fun foo() {
    bar<caret>(name1 = 3, name2 = 2, name3 = 1, name4 = { it })
}

fun bar(name1: Int, name2: Int, name3: Int, name4: Int->Int) : Int {
    return name4(name1) + name2 + name3
}