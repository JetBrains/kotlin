// FIR_IDENTICAL
// BINARY_STUB_ONLY_TEST

package test

sealed interface SealedInterface {
    class Nested : SealedInterface
    object Top : SealedInterface
}
