package sealedFirst

import sealedSecond.SealedInterfaceB

sealed interface <caret>SealedInterfaceA
sealed class <caret>HierarchyClassA: SealedInterfaceA, SealedInterfaceB
