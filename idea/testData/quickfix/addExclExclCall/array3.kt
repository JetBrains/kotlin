// "Add non-null asserted (!!) call" "true"
fun test(a: Array<String?>?): String {
    return <caret>a!![0]
}