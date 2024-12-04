// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -ASSIGNED_VALUE_IS_NEVER_READ
// WITH_EXPERIMENTAL_CHECKERS
// WITH_EXTRA_CHECKERS

fun foo() {
    var boolean = false
    boolean = boolean.not()
}
