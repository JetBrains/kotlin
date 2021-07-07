// KOTLIN_CONFIGURATION_FLAGS: SAM_CONVERSIONS=CLASS
// WITH_SIGNATURES
package test

// We used to generate SAM adapters inside of inline lambdas as
// inline SAM wrappers (`...$sam$i$...`). This is unnecessary, unless
// the code is itself contained in an inline function.
fun test() {
    val lambda = { }
    1.apply { Runnable(lambda) }
}
