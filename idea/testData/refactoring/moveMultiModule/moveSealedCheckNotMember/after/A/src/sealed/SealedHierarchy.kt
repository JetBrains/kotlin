package sealed

sealed interface SealedInterfaceA
sealed interface SealedInterfaceB

sealed class HierarchySealedClass: SealedInterfaceA, SealedInterfaceB
class DerivedFromSealed: HierarchySealedClass()
