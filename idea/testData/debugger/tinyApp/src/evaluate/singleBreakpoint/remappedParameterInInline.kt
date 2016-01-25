package remappedParameterInInline

inline fun watch(p: String, f: (String) -> Int) {
    // EXPRESSION: p
    // RESULT: "mno": Ljava/lang/String;
    //Breakpoint!
    f(p)
}

fun main(args: Array<String>) {
    val local = "mno"
    watch(local) { it.length }
}

