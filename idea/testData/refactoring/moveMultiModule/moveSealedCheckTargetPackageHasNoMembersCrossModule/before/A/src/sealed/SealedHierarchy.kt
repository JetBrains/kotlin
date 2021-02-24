package sealed

sealed interface <caret>SealedInterfaceA
sealed interface SealedInterfaceB

class HierarchyClassA: SealedInterfaceA, SealedInterfaceB