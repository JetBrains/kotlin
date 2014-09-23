var fooBar = ""

object C {
    fun getFooBar() = ""
}

fun f(s: String){
    if (C.getFooBar() == <caret>)
}

// ORDER: fooBar, s
