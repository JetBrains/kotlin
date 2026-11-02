// TARGET_BACKEND: JS_IR, JS_IR_ES6
// FILE: lib.kt
inline fun jsUpper(c: Char) = c.toString().asDynamic().toUpperCase().unsafeCast<String>()

inline fun upper(c: Char): Char = jsUpper(c)[0]

// FILE: main.kt
// EXPECT_GENERATED_JS: function=test expect=tempVarDeclOnAssignmentTest.js IGNORED_MODES=USE_CONST_AND_LET_FOR_VARIABLES
// EXPECT_GENERATED_JS: function=test expect=tempVarDeclOnAssignmentTest.constLet.js TARGET_MODES=USE_CONST_AND_LET_FOR_VARIABLES
fun test(a: Char, b: Char): String {
    return "${upper(a)}${upper(b)}"
}

fun box() = test('o', 'k')
