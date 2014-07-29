//KT-3988 This@label for outer function not resolved

class Comment() {
    var article = ""

}
class Comment2() {
    var article2 = ""
}

fun new(body: Comment.() -> Unit) = body

fun new2(body: Comment2.() -> Unit) = body

fun main(args: Array<String>) {
    new {
        new2 {
            <!UNUSED_EXPRESSION!>this@new<!> //UNRESOLVED REFERENCE
        }
    }
}