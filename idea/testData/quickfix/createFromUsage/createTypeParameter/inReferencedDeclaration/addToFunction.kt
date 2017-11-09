// "Create type parameter in function 'bar'" "true"

fun bar() {

}

fun foo() {
    bar<<caret>String>()
}