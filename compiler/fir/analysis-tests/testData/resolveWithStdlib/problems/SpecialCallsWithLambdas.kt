fun foo() {
    val inv = {{}}<!NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION!>!!<!>
    val bar = {{}}
}
