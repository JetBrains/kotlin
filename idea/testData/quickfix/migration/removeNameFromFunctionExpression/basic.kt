// "Remove identifier from function expression" "true"

fun foo() {
    (fun bar<caret>() {
        return@bar
    })
}
