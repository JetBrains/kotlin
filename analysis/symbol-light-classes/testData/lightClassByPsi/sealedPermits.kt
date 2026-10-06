// JVM_TARGET: 17
package test

sealed interface SealedInterface {
    data object SealedInterfaceBase : SealedInterface
}

sealed class SealedClass {
    class Final : SealedClass()
    object Obj : SealedClass()
}

sealed class SealedWithOpenSubclass
open class OpenSubclass : SealedWithOpenSubclass()
