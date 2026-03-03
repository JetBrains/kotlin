# Java-Direct Module: Implementation Plan

## Overview

Replace IntelliJ platform-based Java parsing with a custom implementation using KMP Java Parser. Goal: eliminate platform dependency while maintaining Java-Kotlin bidirectional interoperability.

**Status**: 90/138 tests passing (65.2%) after iteration 6  
**Last Updated**: 2026-03-03

**Related docs**:
- `AGENT_INSTRUCTIONS.md` — Agent guidelines and iteration workflow
- `ITERATION_RESULTS.md` — Progress history and key findings
- `FIRSESSION_RESOLUTION_ANALYSIS.md` — Type resolution architecture decision

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Kotlin Resolution (FIR)                  │
├─────────────────────────────────────────────────────────────┤
│                    FirJavaFacade                            │
│              (Java Model → FIR converter)                   │
├─────────────────────────────────────────────────────────────┤
│               CombinedJavaClassFinder                        │
│          (source-first, binary-fallback)                    │
├─────────────────────────────────────────────────────────────┤
│              Java Model Implementation                       │
│  (JavaClassOverAst, JavaMethodOverAst, etc.)               │
│              - Lazy type resolution                          │
│              - classifierQualifiedName for FIR              │
├─────────────────────────────────────────────────────────────┤
│                KMP Java Parser                               │
│         (org.jetbrains.java.syntax.jvm)                     │
└─────────────────────────────────────────────────────────────┘
```

### Key Principles

1. **No FirSession in Java Model** — Java Model provides names, FIR resolves them
2. **Lazy evaluation** — Parse and resolve only when needed
3. **Hybrid class finding** — Source classes first, binary fallback via `CombinedJavaClassFinder`
4. **Callback pattern for star imports** — `resolve(tryResolve: (String) -> Boolean)` lets FIR validate existence

---

## Core Components

### Type Resolution Architecture

**Key Decision**: Type resolution happens in the **FIR layer**, not in Java Model.

See `FIRSESSION_RESOLUTION_ANALYSIS.md` for detailed rationale.

**How it works**:
1. Java Model returns `classifier = null` for external types
2. Java Model provides `classifierQualifiedName` (qualified via imports or simple name)
3. FIR uses `session.symbolProvider` to resolve
4. Callback pattern: `resolve(tryResolve: (String) -> Boolean)` for star imports

### Hybrid Class Finding (Iteration 6)

`CombinedJavaClassFinder` in `JavaDirectComponentRegistrar.kt`:
1. Try source class finder (`JavaClassFinderOverAstImpl`)
2. Fall back to binary class finder (default)

This allows finding JDK classes and library classes that aren't in sources.

### Import Handling

`JavaImports` class extracts:
- `simpleImports: Map<String, FqName>` — single-type imports
- `starImports: List<FqName>` — on-demand imports

Star import resolution uses callback pattern: Java Model tries candidates, FIR validates existence.

---

## Milestones

### ✅ Milestone 1: Foundation (Complete)
- Regex-based pre-parsing and indexing
- Basic Java Model classes
- Test infrastructure wiring
- 1/138 tests passing

### ✅ Milestone 2: Resolution (Complete)  
- Import handling with callback pattern
- Type arguments parsing
- Hybrid class finder
- **90/138 tests passing (65.2%)**

### 🔲 Milestone 3: Remaining Issues
Focus areas for 48 failing tests:
- Type parameters (`T`, `U`) being treated as class names
- Complex generics (`? extends`, `? super`)
- SAM lambda inference

### 🔲 Milestone 4: Production Readiness
- Modern Java features (records, sealed classes)
- Performance optimization
- Error handling improvements
- Target: >95% test pass rate

---

## Key Files

| File | Purpose |
|------|---------|
| `JavaClassFinderOverAstImpl.kt` | Source file indexing, class lookup |
| `JavaClassOverAst.kt` | Java class model over AST |
| `JavaTypeOverAst.kt` | Type representations, `classifierQualifiedName` |
| `JavaImports.kt` | Import extraction and resolution |
| `JavaMemberOverAst.kt` | Methods, fields, constructors, parameters |
| `JavaDirectComponentRegistrar.kt` | Plugin registration, hybrid finder |

**FIR integration** (modified in iteration 4-6):
- `JavaTypeConversion.kt` — uses `resolve()` callback
- `javaTypes.kt` — added `isResolved`/`resolve()` to `JavaClassifierType`
- `JavaClassFinderFactory.kt` — added `defaultFinderProvider` parameter

---

## References

**Interfaces**:
- `core/compiler.common.jvm/src/.../load/java/structure/javaElements.kt`
- `core/compiler.common.jvm/src/.../load/java/JavaClassFinder.kt`

**FIR Integration**:
- `compiler/fir/fir-jvm/src/.../FirJavaFacade.kt`
- `compiler/fir/fir-jvm/src/.../JavaSymbolProvider.kt`
- `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt`

**Related Issues**:
- KT-70023: Consider getting rid of KotlinJavaPsiFacade
- OSIP-191: Get rid of IJ platform dependency in Compiler Frontend

---

## Change Log

- 2026-03-03: Condensed document after iteration 6; archived detailed sections
- 2026-02-23: Added type resolution architecture (FIR layer, not Java Model)
- 2026-02-10: Initial version
