package soBreakpointWithInline

fun foo() {}

fun main(args: Array<String>) {
    //Breakpoint!
    12.inlineExt().normalExt() // Try to step over this line. Debugger will stop on the same line twice
    foo()
}

inline fun Any.inlineExt(): Any = this
fun Any.normalExt(): Any = this