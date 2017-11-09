package anonymousFunAsParamDefaultValue

fun foo(a: (String) -> String = fun(b: String): String {
    listOf("").map {
        //Breakpoint!
        val a = it.length
    }

    //Breakpoint!
    return b
}) {
    a("")
}

fun main(args: Array<String>) {
    foo()
}

// RESUME: 2