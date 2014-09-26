// test that name similarity sorting takes over declaration kind sorting
val globalBar = ""
val globalX = ""

fun f(fooBar: String){}

fun g() {
    val localBar = ""
    val localX = ""
    f(<caret>)
}

// ORDER: localBar, globalBar, localX, globalX
