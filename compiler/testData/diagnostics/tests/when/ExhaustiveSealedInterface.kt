// FIR_IDENTICAL

sealed interface SealedInterfaceOverAbstractClass
abstract class BaseAbstractClass
class SealedInterfaceOverAbstractClassImpl1 : BaseAbstractClass(), SealedInterfaceOverAbstractClass
class SealedInterfaceOverAbstractClassImpl2 : BaseAbstractClass(), SealedInterfaceOverAbstractClass

fun testExhaustiveByImplementations(x: SealedInterfaceOverAbstractClass): String {
    return when (x) {
        is SealedInterfaceOverAbstractClassImpl1 -> ""
        is SealedInterfaceOverAbstractClassImpl2 -> ""
    }
}

fun testExhaustiveBySuperClass(x: SealedInterfaceOverAbstractClass): String {
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is BaseAbstractClass -> ""
    }
}

sealed interface SealedInterfaceOverSealedClass
sealed class BaseSealedClass
class SealedInterfaceOverSealedClassImpl1 : BaseSealedClass(), SealedInterfaceOverSealedClass
class SealedInterfaceOverSealedClassImpl2 : BaseSealedClass(), SealedInterfaceOverSealedClass

fun testExhaustiveByImplementations(x: SealedInterfaceOverSealedClass): String {
    return when (x) {
        is SealedInterfaceOverSealedClassImpl1 -> ""
        is SealedInterfaceOverSealedClassImpl2 -> ""
    }
}

fun testExhaustiveBySuperClass(x: SealedInterfaceOverSealedClass): String {
    return when (x) {
        is BaseSealedClass -> ""
    }
}

sealed interface SealedInterfaceOverInterface
interface BaseInterface
class SealedInterfaceOverInterfaceImpl1 : SealedInterfaceOverInterface
class SealedInterfaceOverInterfaceImpl2 : SealedInterfaceOverInterface

fun testExhaustiveByImplementations(x: SealedInterfaceOverInterface): String {
    return when (x) {
        is SealedInterfaceOverInterfaceImpl1 -> ""
        is SealedInterfaceOverInterfaceImpl2 -> ""
    }
}

fun testExhaustiveBySuperInterface(x: SealedInterfaceOverInterface): String {
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is BaseInterface -> ""
    }
}

sealed interface SealedInterfaceOverSealedInterface
sealed interface BaseSealedInterface
class SealedInterfaceOverSealedInterfaceImpl1 : BaseSealedInterface, SealedInterfaceOverSealedInterface
class SealedInterfaceOverSealedInterfaceImpl2 : BaseSealedInterface, SealedInterfaceOverSealedInterface

fun testExhaustiveByImplementations(x: SealedInterfaceOverSealedInterface): String {
    return when (x) {
        is SealedInterfaceOverSealedInterfaceImpl1 -> ""
        is SealedInterfaceOverSealedInterfaceImpl2 -> ""
    }
}

fun testExhaustiveBySuperInterface(x: SealedInterfaceOverSealedInterface): String {
    return when (x) {
        is BaseSealedInterface -> ""
    }
}

sealed interface SealedInterfaceOverDisjointSealedClass
sealed class BaseDisjointSealedClass1
sealed class BaseDisjointSealedClass2
class SealedInterfaceOverDisjointSealedClassImpl1 : BaseDisjointSealedClass1(), SealedInterfaceOverDisjointSealedClass
class SealedInterfaceOverDisjointSealedClassImpl2 : BaseDisjointSealedClass1(), SealedInterfaceOverDisjointSealedClass
class SealedInterfaceOverDisjointSealedClassImpl3 : BaseDisjointSealedClass2(), SealedInterfaceOverDisjointSealedClass
class SealedInterfaceOverDisjointSealedClassImpl4 : BaseDisjointSealedClass2(), SealedInterfaceOverDisjointSealedClass

fun testExhaustiveByImplementations(x: SealedInterfaceOverDisjointSealedClass): String {
    return when (x) {
        is SealedInterfaceOverDisjointSealedClassImpl1 -> ""
        is SealedInterfaceOverDisjointSealedClassImpl2 -> ""
        is SealedInterfaceOverDisjointSealedClassImpl3 -> ""
        is SealedInterfaceOverDisjointSealedClassImpl4 -> ""
    }
}

fun testExhaustiveBySuperClass(x: SealedInterfaceOverDisjointSealedClass): String {
    return when (x) {
        is BaseDisjointSealedClass1 -> ""
        is BaseDisjointSealedClass2 -> ""
    }
}

sealed interface SealedInterfaceOverDisjointSealedInterface
sealed interface BaseDisjointSealedInterface1
sealed interface BaseDisjointSealedInterface2
class SealedInterfaceOverDisjointSealedInterfaceImpl1 : BaseDisjointSealedInterface1, SealedInterfaceOverDisjointSealedInterface
class SealedInterfaceOverDisjointSealedInterfaceImpl2 : BaseDisjointSealedInterface1, SealedInterfaceOverDisjointSealedInterface
class SealedInterfaceOverDisjointSealedInterfaceImpl3 : BaseDisjointSealedInterface2, SealedInterfaceOverDisjointSealedInterface
class SealedInterfaceOverDisjointSealedInterfaceImpl4 : BaseDisjointSealedInterface2, SealedInterfaceOverDisjointSealedInterface

fun testExhaustiveByImplementations(x: SealedInterfaceOverDisjointSealedInterface): String {
    return when (x) {
        is SealedInterfaceOverDisjointSealedInterfaceImpl1 -> ""
        is SealedInterfaceOverDisjointSealedInterfaceImpl2 -> ""
        is SealedInterfaceOverDisjointSealedInterfaceImpl3 -> ""
        is SealedInterfaceOverDisjointSealedInterfaceImpl4 -> ""
    }
}

fun testExhaustiveBySuperInterface(x: SealedInterfaceOverDisjointSealedInterface): String {
    return when (x) {
        is BaseDisjointSealedInterface1 -> ""
        is BaseDisjointSealedInterface2 -> ""
    }
}
