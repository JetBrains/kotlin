# Kotlin PSI (Program Structure Interface)

PSI represents Kotlin source code as a syntax tree. It is the foundation for code analysis, navigation, and refactoring in both the compiler and IDE.

## Relationship with Analysis API

PSI provides **syntax** information (structure of code). Analysis API builds on top of PSI to provide **semantic** information (meaning of code).

```
Source Code → PSI Tree (syntax) → Analysis API (semantics) → Symbols
```

- `KtResolvable` interface marks PSI elements that can be resolved to Analysis API symbols
- When working with PSI, you often need Analysis API to understand what the code means
- See [analysis/AGENTS.md](../../analysis/AGENTS.md) for Analysis API guidelines

## Module Structure

- `psi-api/` - Core PSI interfaces (`KtElement`, `KtExpression`, `KtDeclaration`)
- `psi-impl/` - Implementations and stubs for incremental compilation
- `psi-frontend-utils/` - Compiler integration utilities
- `psi-utils/` - Helper utilities

## Main Classes

```
KtElement (root interface)
├── KtExpression (calls, literals, operators, etc.)
│   ├── KtCallExpression
│   ├── KtBinaryExpression
│   ├── KtLambdaExpression
│   └── ...
└── KtDeclaration (classes, functions, properties)
    ├── KtClass, KtObjectDeclaration
    ├── KtNamedFunction
    ├── KtProperty
    └── ...
```

- `KtFile` - root of a Kotlin file's PSI tree
- `KtPsiFactory` - factory for creating PSI elements programmatically

## Key Patterns

**Visitor pattern** for AST traversal:
- `KtVisitor<R, D>` - base visitor with return type R and data D
- `KtTreeVisitor<D>` - recursive tree traversal

**Stubs** for performance:
- Binary PSI representation for faster parsing
- Used for library files and caching

## PSI Development Rules

### Shared Principles with Analysis API

PSI and Analysis API share common development principles. Before contributing:

→ READ [`analysis/docs/contribution-guide/api-development.md`](../../analysis/docs/contribution-guide/api-development.md) for API design principles
→ READ [`analysis/docs/contribution-guide/api-evolution.md`](../../analysis/docs/contribution-guide/api-evolution.md) for stability and deprecation

### Java-Kotlin Interoperability

**J2K Conversion Limitations:**

Converting Java PSI classes to Kotlin is NOT always possible. Before attempting:

1. **`@JvmName` unavailable in interfaces** — in some cases it is impossible to convert Java methods to Kotlin properties in a binary-compatible way since `@JvmName` cannot be used to fix potential clashes.

2. **Platform type handling** — IntelliJ Platform APIs use Java types extensively; Kotlin's null-safety interop requires careful handling.
   - The classic example is `PsiElement.getParent()` returning `PsiElement!`. After conversion to Kotlin it becomes either `PsiElement?` or `PsiElement` – both of them are breaking changes.
     A workaround is to delegate the implementation to a Java method and keep the return type implicit.

3. **Binary compatibility** — PSI classes are widely used; the binary and source compatibility must be preserved as much as possible.

**Guidance:** Always consult with PSI maintainers before converting Java classes to Kotlin.

### PSI-Specific Notes

**Naming:** All PSI types use the `Kt` prefix (vs `Ka` for Analysis API).

**Stability annotations:**
- `@KtExperimentalApi` — Experimental public API
- `@KtImplementationDetail` — Internal implementation
- `@KtNonPublicApi` — JetBrains-internal APIs
- `@KtPsiInconsistencyHandling` — Code handling inconsistent PSI states

**Java-Kotlin interop:** See the "Java-Kotlin Interoperability" section in [api-development.md](../../analysis/docs/contribution-guide/api-development.md).

**PSI-specific naming patterns:**
- `visit` prefix for visitor methods (e.g., `visitCallExpression`)
- `create` prefix for factory methods in `KtPsiFactory` (e.g., `createExpression`)

## Detailed Documentation

WHEN modifying PSI interfaces or adding new element types:
→ Explore [psi-api/src/org/jetbrains/kotlin/psi/](psi-api/src/org/jetbrains/kotlin/psi/) for existing patterns

WHEN working with PSI visitors:
→ READ [psi-api/src/org/jetbrains/kotlin/psi/KtVisitor.java](psi-api/src/org/jetbrains/kotlin/psi/KtVisitor.java)
→ READ [psi-api/src/org/jetbrains/kotlin/psi/KtTreeVisitor.java](psi-api/src/org/jetbrains/kotlin/psi/KtTreeVisitor.java)

WHEN creating PSI elements programmatically:
→ READ [psi-api/src/org/jetbrains/kotlin/psi/KtPsiFactory.kt](psi-api/src/org/jetbrains/kotlin/psi/KtPsiFactory.kt)
