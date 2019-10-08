package smartStepIntoComponentFunction

data class Test(val a: Int, val b: Int)

fun main(args: Array<String>) {
    val t = Test(12, 2)
    // SMART_STEP_INTO_BY_INDEX: 1
    // RESUME: 1
    //Breakpoint!
    t.component2()
}