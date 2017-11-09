fun foo(): Int {
    println()
    return <before><caret>
}

val xxx<change>

// TYPE: " = 1"
// COMPLETION_TYPE: SMART
// EXIST: xxx
