// "Remove redundant semicolon" "true"
fun foo() {
    a();<caret>
    b()
}

fun a(){}
fun b(){}