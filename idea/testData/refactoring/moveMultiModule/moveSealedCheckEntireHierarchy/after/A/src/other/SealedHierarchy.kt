package other

sealed interface SealedInterfaceA
sealed interface SealedInterfaceB
sealed class HierarchyClassA: SealedInterfaceA, SealedInterfaceB
class NonSealedButMember: HierarchyClassA()