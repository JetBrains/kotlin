# Tests for synthetic accessor generation on KLIB-based backends

On KLIB-based backends, private functions can leak through internal inline functions to other files or even modules
(if the internal inline function is called from a friend module).
Such private functions (that are called from inline functions with higher visibility) must be wrapped in synthetic
accessor functions that have the highest visibility of all the inline functions that transitively call that private function.

To ensure that all the needed synthetic accessors are indeed generated, we use two techniques:
- [IrVisibilityChecker](../../../ir/backend.common/src/org/jetbrains/kotlin/backend/common/IrVisibilityChecker.kt).
  It traverses the IR tree and verifies that private and local declarations are never accessed from a file other than
  where they're declared.
- [Dumping the list of synthetic accessors](../../../ir/ir.inline/src/org/jetbrains/kotlin/ir/inline/DumpSyntheticAccessors.kt)
  after generating them.

Tests in this directory include various combinations and corner cases to verify the synthetic accessor generation logic in
[SyntheticAccessorLowering](../../../ir/ir.inline/src/org/jetbrains/kotlin/ir/inline/SyntheticAccessorLowering.kt).

**NOTE:** We only compile these tests up to and including the last IR lowering, which means that we don't run LLVM (on Kotlin/Native)
and obviously don't run the executable (on Kotlin/Native there is no executable).
