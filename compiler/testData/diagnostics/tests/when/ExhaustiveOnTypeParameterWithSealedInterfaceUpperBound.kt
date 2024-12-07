// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ImprovedExhaustivenessChecksIn21}

package sealedInterface

sealed interface SealedI

class SimpleClass1: SealedI
class SimpleClass2: SealedI

enum class EnumClass : SealedI { A, B }

data class DataClass(val value: Int): SealedI

fun <T: SealedI> testInstance(value: T) = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    is SimpleClass1 -> 1
    is SimpleClass2 -> 2
    EnumClass.A -> 3
    EnumClass.B -> 4
    is DataClass -> 5
}

fun <T: SealedI> testInstanceAgain(value: T) = <!NO_ELSE_IN_WHEN!>when<!> (value) {
    is SimpleClass1 -> 1
    is SimpleClass2 -> 2
    is EnumClass -> 3
    is DataClass -> 4
}

fun <T : SealedI> T.extensionFun() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
    is SimpleClass1 -> 1
    is SimpleClass2 -> 2
    EnumClass.A -> 3
    EnumClass.B -> 4
    is DataClass -> 5
}

inline fun <reified T : SealedI> T.inlineExtensionFun() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
    is SimpleClass1 -> 1
    is SimpleClass2 -> 2
    EnumClass.A -> 3
    EnumClass.B -> 4
    is DataClass -> 5
}

val <T : SealedI> T.extensionPropWithGetter
    get() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
        is SimpleClass1 -> 1
        is SimpleClass2 -> 2
        EnumClass.A -> 3
        EnumClass.B -> 4
        is DataClass -> 5
    }

class Test<T: SealedI> {
    fun testInstance(value: T) = <!NO_ELSE_IN_WHEN!>when<!>(value) {
        is SimpleClass1 -> 1
        is SimpleClass2 -> 2
        EnumClass.A -> 3
        EnumClass.B -> 4
        is DataClass -> 5
    }
    fun T.extensionFun() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
        is SimpleClass1 -> 1
        is SimpleClass2 -> 2
        EnumClass.A -> 3
        EnumClass.B -> 4
        is DataClass -> 5
    }
    val T.extensionPropWithGetter
        get() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
            is SimpleClass1 -> 1
            is SimpleClass2 -> 2
            EnumClass.A -> 3
            EnumClass.B -> 4
            is DataClass -> 5
        }
}

class Inv<T>(val prop: T)

fun <T: SealedI> testOut(instance: Inv<out T>) = <!NO_ELSE_IN_WHEN!>when<!>(instance.prop) {
    is SimpleClass1 -> 1
    is SimpleClass2 -> 2
    EnumClass.A -> 3
    EnumClass.B -> 4
    is DataClass -> 5
}
