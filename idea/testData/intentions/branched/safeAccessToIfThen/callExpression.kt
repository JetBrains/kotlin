fun foo(): String? = "foo"
fun main(args: Array<String>) {
    foo()?.<caret>length
}
