# Common syntax definitions for Kotlin frontends

A lightweight Kotlin Multiplatform module (JVM and Wasm) with no IntelliJ platform dependencies. It holds the syntax-level
definitions shared by all three Kotlin syntax trees:

* PSI (`compiler/psi`)
* LightTree (`PsiBuilder`-based light tree, `compiler/fir/raw-fir/light-tree2fir`)
* KMP (the multiplatform parser, `compiler/multiplatform-parsing`)

The module is part of getting rid of the IntelliJ platform dependency in the compiler frontend.

## Purpose

### Syntax element identifiers

The module holds the list of all syntax element identifiers, which are consistent across all three frontends:

* `BasicSyntaxElementTypeIds` (including `NO_ID` for element types without an ID)
* `KotlinSyntaxTokenIds`
* `KotlinSyntaxNodeTypeIds`
* `KDocSyntaxTokenIds`
* `KDocSyntaxNodeTypeIds`

An identifier has the same name as the element type it's assigned to (e.g., `KotlinSyntaxTokenIds.LBRACE` for `KtTokens.LBRACE`).
Always reference identifiers qualified with their object instead of importing them by name: an imported `LBRACE` would
clash with the element type of the same name.

The element types of every frontend take their IDs from here:

* PSI tokens (`org.jetbrains.kotlin.lexer.KtTokens`, `org.jetbrains.kotlin.kdoc.lexer.KDocTokens`) pass them to their
  constructors.
* Other PSI element types (`org.jetbrains.kotlin.KtNodeTypes` and the language-independent `com.intellij.psi.TokenType`
  ones) don't take an ID in their constructors, so they are mapped to it by `IElementType.syntaxId()` in `compiler:psi:psi-frontend-utils`.
* KMP element types (`org.jetbrains.kotlin.kmp.lexer.BasicElementTypes`, `org.jetbrains.kotlin.kmp.lexer.KtTokens`,
  `org.jetbrains.kotlin.kmp.parser.KtNodeTypes`, ...) are registered with them.

Syntax checkers, positioning strategies and other utilities should rely on these identifiers rather than on
frontend-specific element type classes (`IElementType` for PSI and LightTree, `SyntaxElementType` for KMP). This way the
same code works for any tree, and the dependency on the IntelliJ platform classes can be dropped.

The identifiers are `const val`s, so their values are inlined at compile time. This allows `when` expressions over node
types to be compiled into fast table/lookup switches.

## Rules for identifiers

* IDs are contiguous and start from `BasicSyntaxElementTypeIds.NO_ID` (`0`). The first ID of each object continues
  the last ID of the previous one (`BasicSyntaxElementTypeIds` → `KotlinSyntaxTokenIds` → `KotlinSyntaxNodeTypeIds` →
  `KDocSyntaxTokenIds` → `KDocSyntaxNodeTypeIds`). Remember to update the first constant of the next object after adding a new identifier.
* Keep the IDs compact, without gaps or reserved ranges: the upcoming `SyntaxIdSet` is supposed to store IDs as bits
  indexed by their values, so its size and efficiency depend on the maximum ID.
* All identifiers are compile-time constants (`const val`): the compiler computes their values and inlines them at
  every use site. So the cross-object references above (e.g., the first ID of `KotlinSyntaxNodeTypeIds` refers to the
  last ID of `KotlinSyntaxTokenIds`) never trigger object initialization at runtime, and there is no risk of an
  initialization cycle between the objects. Keep new identifiers `const` to preserve this.
* `SyntaxIdsTest` checks that all identifiers are `const`, unique, and form a contiguous range.
* Every identifier must be unique.
* Element types with the same identifier must represent the same syntax element in all frontends.
