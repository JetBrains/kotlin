fun foo() {
    val test : <caret>
}

/*TODO: Is 'package' type qualifier syntax correct?*/
// EXIST: package
// EXIST: dynamic
// NOTHING_ELSE: true
