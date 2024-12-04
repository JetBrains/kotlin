// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ImprovedExhaustivenessChecksIn21

sealed class SealedClass

class SealedSubClass1 : SealedClass()
class SealedSubClass2 : SealedClass()
object SealedSubObject1 : SealedClass()
object SealedSubObject2 : SealedClass()

fun <T: SealedClass> testInstance(value: T) = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    is SealedSubClass1 -> 1
    is SealedSubClass2 -> 2
    is SealedSubObject1 -> 3
    is SealedSubObject2 -> 4
}

fun <T: SealedClass?> testInstanceDnn(value: T & Any) = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    is SealedSubClass1 -> 1
    is SealedSubClass2 -> 2
    is SealedSubObject1 -> 3
    is SealedSubObject2 -> 4
}

fun <T : SealedClass> T.extensionFun() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
    is SealedSubClass1 -> 1
    is SealedSubClass2 -> 2
    is SealedSubObject1 -> 3
    is SealedSubObject2 -> 4
}

inline fun <reified T : SealedClass> T.inlineExtensionFun() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
    is SealedSubClass1 -> 1
    is SealedSubClass2 -> 2
    is SealedSubObject1 -> 3
    is SealedSubObject2 -> 4
}

val <T : SealedClass> T.extensionPropWithGetter
    get() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
        is SealedSubClass1 -> 1
        is SealedSubClass2 -> 2
        is SealedSubObject1 -> 3
        is SealedSubObject2 -> 4
    }

class C<T: SealedClass> {
    val T.extensionPropWithGetter
        get() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
            is SealedSubClass1 -> 1
            is SealedSubClass2 -> 2
            is SealedSubObject1 -> 3
            is SealedSubObject2 -> 4
        }

    fun testInstance(value: T) = <!NO_ELSE_IN_WHEN!>when<!>(value) {
        is SealedSubClass1 -> 1
        is SealedSubClass2 -> 2
        is SealedSubObject1 -> 3
        is SealedSubObject2 -> 4
    }

    fun T.extensionFun() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
        is SealedSubClass1 -> 1
        is SealedSubClass2 -> 2
        is SealedSubObject1 -> 3
        is SealedSubObject2 -> 4
    }
}

class Inv<T>(val prop: T)

fun <T: SealedClass> testOut(instance: Inv<out T>) = <!NO_ELSE_IN_WHEN!>when<!>(instance.prop) {
    is SealedSubClass1 -> 1
    is SealedSubClass2 -> 2
    is SealedSubObject1 -> 3
    is SealedSubObject2 -> 4
}

class TestOut<out T> where T: SealedClass {
    fun testOut(instance: @UnsafeVariance T) = <!NO_ELSE_IN_WHEN!>when<!>(instance) {
        is SealedSubClass1 -> 1
        is SealedSubClass2 -> 2
        is SealedSubObject1 -> 3
        is SealedSubObject2 -> 4
    }
}