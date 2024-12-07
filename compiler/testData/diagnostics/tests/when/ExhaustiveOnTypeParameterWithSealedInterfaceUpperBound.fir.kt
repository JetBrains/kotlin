// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ImprovedExhaustivenessChecksIn21}

package sealedInterface

sealed interface SealedI

class SimpleClass1: SealedI
class SimpleClass2: SealedI

enum class EnumClass : SealedI { A, B }

data class DataClass(val value: Int): SealedI

fun <T: SealedI> testInstance(value: T) = when(value) {
    is SimpleClass1 -> 1
    is SimpleClass2 -> 2
    EnumClass.A -> 3
    EnumClass.B -> 4
    is DataClass -> 5
}

fun <T: SealedI> testInstanceAgain(value: T) = when (value) {
    is SimpleClass1 -> 1
    is SimpleClass2 -> 2
    is EnumClass -> 3
    is DataClass -> 4
}

fun <T : SealedI> T.extensionFun() = when (this) {
    is SimpleClass1 -> 1
    is SimpleClass2 -> 2
    EnumClass.A -> 3
    EnumClass.B -> 4
    is DataClass -> 5
}

inline fun <reified T : SealedI> T.inlineExtensionFun() = when (this) {
    is SimpleClass1 -> 1
    is SimpleClass2 -> 2
    EnumClass.A -> 3
    EnumClass.B -> 4
    is DataClass -> 5
}

val <T : SealedI> T.extensionPropWithGetter
    get() = when (this) {
        is SimpleClass1 -> 1
        is SimpleClass2 -> 2
        EnumClass.A -> 3
        EnumClass.B -> 4
        is DataClass -> 5
    }

class Test<T: SealedI> {
    fun testInstance(value: T) = when(value) {
        is SimpleClass1 -> 1
        is SimpleClass2 -> 2
        EnumClass.A -> 3
        EnumClass.B -> 4
        is DataClass -> 5
    }
    fun T.extensionFun() = when (this) {
        is SimpleClass1 -> 1
        is SimpleClass2 -> 2
        EnumClass.A -> 3
        EnumClass.B -> 4
        is DataClass -> 5
    }
    val T.extensionPropWithGetter
        get() = when (this) {
            is SimpleClass1 -> 1
            is SimpleClass2 -> 2
            EnumClass.A -> 3
            EnumClass.B -> 4
            is DataClass -> 5
        }
}

class Inv<T>(val prop: T)

fun <T: SealedI> testOut(instance: Inv<out T>) = when(instance.prop) {
    is SimpleClass1 -> 1
    is SimpleClass2 -> 2
    EnumClass.A -> 3
    EnumClass.B -> 4
    is DataClass -> 5
}