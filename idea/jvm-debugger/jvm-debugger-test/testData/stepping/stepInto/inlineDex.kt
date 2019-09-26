package inlineDex

fun main(args: Array<String>) {
    //Breakpoint!
    myPrint("OK")
}

inline fun myPrint(s: String) {
    val z = s;
}