// FIR_IDENTICAL
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6

// MODULE: lib1
// FILE: A.kt
const val JS_CODE = """
        var usageOfTheFirstValue = value + 4;
        var usageOfTheComplexValue = complexValue[4];
        console.log("The first result is: " + firstFunction());"""

inline fun firstTest(
    value: Int,
    complexValue: List<Map<String, Int>>,
    firstFunction: () -> Int,
    secondFunction: String.(Int) -> Int,
    thirdWhichWillBeShadowed: (String, String) -> String,
    crossinline crossInlineOne: () -> String,
    noinline noInlineOne: () -> List<Int>
) {
    js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>JS_CODE<!>)
    js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>"console.log('The second is: ' + secondFunction());"<!>)
    js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>"console.log('The third results is: ' + crossInlineOne());"<!>)
    js("""
        var thirdWhichWillBeShadowed = 44;
        console.log("The valid result should be: " + noInlineOne() + ", " + thirdWhichWillBeShadowed());
    """)
}

fun secondTest(
    value: Int,
    complexValue: List<Map<String, Int>>,
    firstFunction: () -> Int,
    secondFunction: String.(Int) -> Int,
    thirdWhichWillBeShadowed: (String, String) -> String,
    crossInlineOne: () -> String,
    noInlineOne: () -> List<Int>
) {
    js(JS_CODE)
    js("console.log('The second is: ' + secondFunction());")
    js("console.log('The third results is: ' + crossInlineOne());")
    js("""
        console.log("The valid result should be: " + noInlineOne() + ", " + thirdWhichWillBeShadowed());
    """)
}

// MODULE: lib2
// FILE: B.kt
inline fun theOnlyFunction(
    value: Int,
    complexValue: List<Map<String, Int>>,
    firstFunction: () -> Int,
    thirdWhichWillBeShadowed: (String, String) -> String,
    crossinline crossInlineOne: () -> String,
    noinline noInlineOne: () -> List<Int>
) {
    js("""
        var usageOfTheFirstValue = value + 4;
        var usageOfTheComplexValue = complexValue[4];
    """)
    js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>"console.log('The first result is: ' + firstFunction());"<!>)
    js(<!JS_CODE_CAPTURES_INLINABLE_FUNCTION_WARNING!>"console.log('The third results is: ' + crossInlineOne());"<!>)
    js("""
        var thirdWhichWillBeShadowed = 44;
        console.log("The valid result should be: " + noInlineOne() + ", " + thirdWhichWillBeShadowed());
    """)
}
