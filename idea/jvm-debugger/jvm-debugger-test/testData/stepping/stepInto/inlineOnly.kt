package inlineOnly

fun main(args: Array<String>) {
    //Breakpoint!
    myPrint("OK")

    forEach { print2("123")}

    println("OK")  //stdlib test
}

fun print2(s: String){
    val z = s;
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline fun myPrint(s: String) {
    val z = s;
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline fun forEach(s: () -> Unit) {
    for (i in 1..2) {
        s()
    }
}

// STEP_INTO: 9
// TRACING_FILTERS_ENABLED: false