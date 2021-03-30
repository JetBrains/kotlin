package sealed

sealed interface SealedInterfaceA
sealed interface SealedInterfaceB

class <caret>HierarchyClassA: SealedInterfaceA, SealedInterfaceB