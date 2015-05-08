// this test tests correct substitution equality checking for generic functions
import dependency.pair

fun foo() {
    1 pai<caret>
}

// EXIST: pair
// NUMBER: 1
