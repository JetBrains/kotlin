// "Add parameter to function 'called'" "true"
// WITH_RUNTIME
// DISABLE-ERRORS
// WITH_NEW_INFERENCE

fun caller() {
    called(<caret>setOf(1, 2, 3))
}

fun called() {}