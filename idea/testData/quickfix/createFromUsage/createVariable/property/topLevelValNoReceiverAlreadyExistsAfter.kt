// "Create property 'foo'" "true"
// ERROR: Property must be initialized

fun test(): Int {
    return <caret>foo
}

val bar = 1
