# Kotlin PSI (Program Structure Interface)

PSI represents Kotlin source code as a syntax tree. It is the foundation for code analysis, navigation, and refactoring in both the compiler and IDE.

## Relationship with Analysis API

PSI provides **syntax** information (structure of code). Analysis API builds on top of PSI to provide **semantic** information (meaning of code).

```
Source Code → PSI Tree (syntax) → Analysis API (semantics) → Symbols
```

- `KtResolvable` interface marks PSI elements that can be resolved to Analysis API symbols
- When working with PSI, you often need Analysis API to understand what the code means
- See `analysis/AGENTS.md` for Analysis API guidelines

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

## API Annotations

- `@KtExperimentalApi` - experimental public API (may change)
- `@KtImplementationDetail` - internal implementation (do not use)
- `@KtNonPublicApi` - JetBrains-internal APIs

## Detailed Documentation

WHEN modifying PSI interfaces or adding new element types:
→ Explore `psi-api/src/org/jetbrains/kotlin/psi/` for existing patterns

WHEN working with PSI visitors:
→ READ `psi-api/src/org/jetbrains/kotlin/psi/KtVisitor.java`
→ READ `psi-api/src/org/jetbrains/kotlin/psi/KtTreeVisitor.java`

WHEN creating PSI elements programmatically:
→ READ `psi-api/src/org/jetbrains/kotlin/psi/KtPsiFactory.kt`
