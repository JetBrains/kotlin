package constantConditions

fun main() {
    trueIf()
    falseIf()
    elseIf()
    whileFalse()
    whileTrue()
}

private fun trueIf() {
    //Breakpoint!
    if (true)
        foo("true")
    else
        foo("false")
}

private fun falseIf() {
    //Breakpoint!
    if(false)
        foo("false")
    else
        foo("true")
}

private fun elseIf() {
    //Breakpoint!
    if (false)
        foo("false")
    //Breakpoint!
    else if (true)
        foo("true")
}


private fun whileFalse() {
    //Breakpoint!
    while (false)
        foo("while false")
}


private fun whileTrue() {
    //Breakpoint!
    while (true)
        break
}

private fun foo(text: String) {}

// RESUME: 6