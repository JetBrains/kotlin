package sealedFirst

import sealedSecond.SealedInterfaceB

sealed interface SealedInterfaceA
sealed class <caret>HierarchyClassA: SealedInterfaceA, SealedInterfaceB
