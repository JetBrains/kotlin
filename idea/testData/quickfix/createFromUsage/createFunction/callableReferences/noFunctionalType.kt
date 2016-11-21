// "Create function 'foo'" "false"
// ACTION: Convert to expression body
// ACTION: Rename reference
// ACTION: Convert reference to lambda
// ERROR: Unresolved reference: foo
fun bar(n: Int) = "$n"

fun consume(s: String) {}

fun test() {
    consume(bar(::<caret>foo))
}