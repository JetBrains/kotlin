// "Create property 'foo'" "true"
// ERROR: Property must be initialized

val bar = 1

fun test(): Int {
    return <caret>foo
}
