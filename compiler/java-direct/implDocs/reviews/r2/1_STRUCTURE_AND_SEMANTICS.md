# Java-Direct Module: Overall Structure and Semantics

## 1. High-Level Architecture
The `java-direct` module is designed to bridge the gap between Java source files (parsed via a KMP-based AST parser) and the Kotlin compiler's FIR (Front-end Intermediate Representation) layer. By bypassing the traditional PSI model for source Java code, the implementation aims for more direct, lightweight, and precise semantic conversions.

The architecture is split into two primary layers:
- **The Java Model (AST Abstraction)**: Classes like `JavaClassOverAst`, `JavaTypeOverAst`, and `JavaMemberOverAst` provide a semantic wrapper over the raw Java AST nodes, offering standard interfaces expected by the compiler (`JavaClass`, `JavaType`, etc.).
- **The FIR Integration Layer**: Files like `FirJavaFacade.kt` and `JavaTypeConversion.kt` convert this Java Model into corresponding FIR symbols.

## 2. Semantic Resolution Strategy
A core tenet of the implementation is the **Callback Pattern for Resolution**. To avoid the ambiguity of string-based fully qualified names (e.g., `"a.b.C"` could mean package `a.b` and class `C`, or package `a` and nested class `b.C`), the Java Model resolves types directly into precise `ClassId` instances.
- **`JavaResolutionContext`**: This central context object manages the scope of any given AST node. It is responsible for JLS-compliant type resolution priority, including explicit imports, same-package lookups, `java.lang` implicits, star imports, and inherited nested classes.
- **FIR Callbacks**: The `JavaResolutionContext` uses a `tryResolve: (ClassId) -> Boolean` callback passed from FIR (e.g., `symbolProvider.getClassLikeSymbolByClassId`). This keeps the Java Model completely unaware of FIR internals while allowing it to drive the complex Java resolution logic.

## 3. Class Finding
The `CombinedJavaClassFinder` acts as the orchestrator for locating classes. It first delegates to `JavaClassFinderOverAstImpl` (which indexes and parses local source files) and falls back to binary-based class finders for JDK or third-party library dependencies.

## 4. Alignment with Initial Goals
The implementation successfully achieves the goal of a standalone Java source model. By rigorously modeling Java language rules (such as implicit supertypes for Enums and Annotations, and precise type-use annotation filtering) and clearly delineating the boundary between "Java semantics" and "FIR translation" (via `ClassId`), the codebase provides a robust, correct-by-construction mapping that satisfies the FIR constraints.
