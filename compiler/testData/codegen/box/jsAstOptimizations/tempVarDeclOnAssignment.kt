// TARGET_BACKEND: JS_IR, JS_IR_ES6
// FILE: lib.kt
inline fun jsUpper(c: Char) = c.toString().asDynamic().toUpperCase().unsafeCast<String>()

inline fun upper(c: Char): Char = jsUpper(c)[0]

// FILE: main.kt
// EXPECT_GENERATED_JS: function=test expect=tempVarDeclOnAssignmentTest.js TARGET_BACKENDS=JS_IR
// EXPECT_GENERATED_JS: function=test expect=tempVarDeclOnAssignmentTest.es6.js TARGET_BACKENDS=JS_IR_ES6
fun test(a: Char, b: Char): String {
    return "${upper(a)}${upper(b)}"
}

fun box() = test('o', 'k')
