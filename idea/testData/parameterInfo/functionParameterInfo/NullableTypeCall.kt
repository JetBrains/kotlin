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
Text: (x: jet.Int), Disabled: false, Strikeout: false, Green: false
Text: (x: jet.Int, y: jet.Int), Disabled: false, Strikeout: false, Green: false
*/