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

**NOTE:** 
* Test runners `*KlibSyntheticAccessorsTestGenerated` only compile these tests up to Klib serialization,
which means that LLVM (on Kotlin/Native) is not run,
and obviously the executable is not created and not executed.
* Test runners `*KlibSyntheticAccessorsBoxTestGenerated` perform usual codegen/box tests, which check
  * general IR Inliner's correctness,
  * correct generation of synthetic accessors in 2nd compilation stage for native caches.
