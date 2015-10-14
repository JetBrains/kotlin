package customLibClassName

fun main(args: Array<String>) {
    customLib.oneFunSameClassName.oneFunSameFileNameFun()
    customLib.twoFunDifferentSignature.twoFunDifferentSignatureFun()
    customLib.property.foo
    customLib.breakpointOnLocalProperty.breakpointOnLocalPropertyFun()
    customLib.simpleLibFile.foo()
}

// ADDITIONAL_BREAKPOINT: 1.kt:public fun oneFunSameFileNameFun(): Int {
// EXPRESSION: 1 + 1
// RESULT: 2: I

// ADDITIONAL_BREAKPOINT: 1.kt:public fun twoFunDifferentSignatureFun(): Int {
// EXPRESSION: 1 + 2
// RESULT: 3: I

// ADDITIONAL_BREAKPOINT: 1.kt:public val foo: Int =
// EXPRESSION: 1 + 3
// RESULT: 4: I

// ADDITIONAL_BREAKPOINT: 1.kt:public fun breakpointOnLocalPropertyFun(): Int {
// EXPRESSION: 1 + 4
// RESULT: 5: I

// ADDITIONAL_BREAKPOINT: simpleLibFile.kt:public fun foo() {
// EXPRESSION: 1 + 5
// RESULT: 6: I