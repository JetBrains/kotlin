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

* `KtTokenId`
* `KtNodeTypeId`
* `KDocTokenId`
* `KDocNodeTypeId`
* `NO_ID`

An identifier has the same name as the element type it's assigned to (e.g., `KtTokenId.LBRACE` for `KtTokens.LBRACE`).
Always reference identifiers qualified with their object instead of importing them by name: an imported `LBRACE` would
clash with the element type of the same name.

The element types of every frontend take their IDs from here:

* PSI tokens (`org.jetbrains.kotlin.lexer.KtTokens`, `org.jetbrains.kotlin.kdoc.lexer.KDocTokens`) pass them to their
  constructors.
* KMP element types (`org.jetbrains.kotlin.kmp.lexer.KtTokens`, `org.jetbrains.kotlin.kmp.parser.KtNodeTypes`, ...) are
  registered with them.

Syntax checkers, positioning strategies and other utilities should rely on these identifiers rather than on
frontend-specific element type classes (`IElementType` for PSI and LightTree, `SyntaxElementType` for KMP). This way the
same code works for any tree, and the dependency on the IntelliJ platform classes can be dropped.

The identifiers are `const val`s. Java code (e.g., `psi-api`, `psi:parser`) gets their values inlined at compile time,
so such modules only need a `compileOnly` dependency on this module.
The identifiers also allow `when` expressions over node types to be compiled into fast table/lookup switches.

### Common tree abstractions

The module is also intended to hold common and abstract classes for syntax trees that don't depend on a specific
frontend, so that the light tree-based utilities and FIR can work with any tree:

* `KtSourceElement` and related classes (`KtSourceElementKind`, `AbstractKtSourceElement`, ...)
* A platform-independent replacement for `LighterASTNode`, which exposes the node ID
  (`tokenId`) instead of an `IElementType`
* A platform-independent replacement for `FlyweightCapableTreeStructure`
* Small utilities needed by them (e.g., `Ref`)

## Rules for identifiers

* IDs are contiguous and start from `NO_ID` (`0`). The first ID of each object continues the last ID of the previous
  one (`KtTokenId` → `KtNodeTypeId` → `KDocTokenId` → `KDocNodeTypeId`). Remember to update the first constant of the next
  object after adding a new identifier.
* Every identifier must be unique.
* Element types with the same identifier must represent the same syntax element in all frontends.
