package localFunction

fun main() {
    //Breakpoint!
    fun bar() {
        "OK"
    }
    bar()
}

// STEP_OVER: 1
// Code compiled on the IR cannot break on local function declarations