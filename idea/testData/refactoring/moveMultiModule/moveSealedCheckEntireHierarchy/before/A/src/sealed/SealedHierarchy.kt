package sealed

sealed interface <caret>SealedInterfaceA
sealed interface <caret>SealedInterfaceB

sealed class <caret>HierarchyClassA: SealedInterfaceA, SealedInterfaceB
class <caret>NonSealedButMember: HierarchyClassA()