sealed class SealedClass { // (1): top level sealed class
    class NestedInheritorA: SealedClass() {
        class NestedNestedInheritorA: SealedClass()
        object NestedNestedInheritorB: SealedClass()
    }
}

class ClassSameFileInheritorA: SealedClass()
class ClassSameFileInheritorB: SealedClass()
object ClassSameFileInheritorC: SealedClass()

sealed interface SealedInterface { // (2): top level sealed interface
    class NestedInheritorA: SealedInterface {
        interface NestedNestedInheritorA: SealedInterface
        object NestedNestedInheritorB: SealedInterface
    }
}

class InterfaceSameFileInheritorA: SealedInterface
class InterfaceSameFileInheritorB: SealedInterface
object InterfaceSameFileInheritorC: SealedInterface


class NonSealedClass {
    sealed class NestedSealedClass // (3): nested sealed class
    sealed interface NestedSealedInterface // (4): nested sealed interface
    class NestedSealedInheritorA: NestedSealedClass()
    class NestedSealedInheritorB: NestedSealedInterface
}