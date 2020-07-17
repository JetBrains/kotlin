package test

fun main(args: Array<String>) {
    //Breakpoint!
    call()
    foo()
}

fun call(a: Int = def()) {
    foo()
}

fun def(): Int {
    return libFun() // 'Step over' causes debugger skipping call()
}

fun foo() {}
fun libFun() = 12

// STEP_INTO: 2
// STEP_OVER: 5