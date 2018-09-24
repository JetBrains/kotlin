// "Remove '=' token from function declaration" "true"

fun foo() {
    <caret>bar()
}

fun bar() = {

}