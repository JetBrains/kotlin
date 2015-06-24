// INTENTION_TEXT: "Add 'b =' to argument"

fun foo(s: String, b: Boolean){}

fun bar() {
    foo("", true<caret>)
}