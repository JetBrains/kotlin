// "Create function 'foo'" "true"
fun foo(n: Int) {}

fun test() {
    foo("a<caret>bc${1}")
}