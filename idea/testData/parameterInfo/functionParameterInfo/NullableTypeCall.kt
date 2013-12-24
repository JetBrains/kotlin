class A()

fun A.index(x: Int) : Int {
    return 1
}
fun A.index(x: Int, y: Int) : Int {
    return x + y
}

fun f() {
    val command : A? = null
    if (command != null){
        command.index(<caret>)
    }
}
/*
Text: (<highlight>x: Int</highlight>), Disabled: false, Strikeout: false, Green: false
Text: (<highlight>x: Int</highlight>, y: Int), Disabled: false, Strikeout: false, Green: false
*/