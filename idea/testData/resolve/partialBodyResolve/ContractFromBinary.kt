// COMPILER_ARGUMENTS: -XXLanguage:+ReadDeserializedContracts -XXLanguage:+UseReturnsEffect
package test

fun irrelevantConsume(y: Any?) {}

fun testContractFromBinaryDependency(x: Any?, y: Any?) {
    require(x is String)

    require(y is String)
    irrelevantConsume(x)

    <caret>x.length

    require(x is Int)
}