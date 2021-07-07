// KOTLIN_CONFIGURATION_FLAGS: SAM_CONVERSIONS=CLASS
// WITH_SIGNATURES
package test

// SAM adapters inside of private inline functions don't need to be public,
// since we do not refer to them from other packages.
fun test() {
    val lambda = { }
    1.apply { Runnable(lambda) }
}
