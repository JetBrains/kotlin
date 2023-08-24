// IGNORE_BACKEND_K2: JVM_IR
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// !IGNORE_ERRORS
// DIAGNOSTICS: -UNRESOLVED_REFERENCE -OVERLOAD_RESOLUTION_AMBIGUITY

val test1 = unresolved

val test2: Unresolved =
        unresolved()

val test3 = 42.unresolved(56)

val test4 = 42 *<!SYNTAX!><!>
