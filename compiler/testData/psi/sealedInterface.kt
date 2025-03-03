// FIR_IDENTICAL
package test

sealed interface SealedInterface {
    class Nested : SealedInterface
    object Top : SealedInterface
}
