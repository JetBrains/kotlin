# Resolution Pipeline: Type Names → ClassId

This document maps the exact call chain for how Java type references in source code become `ClassId` values in FIR. Understanding the pipeline end-to-end is **essential** before attempting any resolution-related fix.

---

## Pipeline Overview

```
Java source text: "a.b.C<D>"
      │
      ▼
┌─────────────────────────────────────┐
│ KMP Parser → AST                    │  JavaTypeOverAst.kt
│  JAVA_CODE_REFERENCE node           │
│  rawTypeName = extractTypeName(node)│  Collects IDENTIFIERs, ignores annotations
│  Result: "a.b.C" (String)          │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│ JavaClassifierTypeOverAst.resolve() │  JavaTypeOverAst.kt:271
│  Delegates to:                      │
│  resolutionContext.resolve(          │
│    rawTypeName, tryResolve)          │
│  Returns: ClassId?                  │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│ JavaResolutionContext.resolve()      │  JavaResolutionContext.kt:177
│                                     │
│ If name contains '.':               │
│   → resolveNestedClassToClassId()   │  Tries outer-class-first (JLS 6.5.2)
│ Else:                               │
│   → resolveSimpleNameToClassId()    │  Imports → same-pkg → java.lang → stars
│                                     │
│ Each candidate is tested via         │
│ tryResolve: (ClassId) → Boolean     │
│ Returns: ClassId? (precise boundary)│
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│ FIR: resolveTypeName()              │  JavaTypeConversion.kt:359
│                                     │
│ 1. Calls javaType.resolve(callback) │  callback = symbolProvider.get...!=null
│    If ClassId returned → done       │
│                                     │
│ 2. Fallback: findClassId(name, ses) │  Probes all pkg/class splits
│    Longest-package-first order      │  For "a.b": tries ClassId("a","b")
│                                     │             then ClassId("","a.b")
│                                     │
│ 3. Last resort:                     │
│    ClassId.topLevel(FqName(name))   │  Treats entire name as FQN
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│ session.symbolProvider               │
│   .getClassLikeSymbolByClassId(id)  │
│                                     │
│ Looks up source + binary classes    │
│ Returns: FirClassLikeSymbol?        │
└─────────────────────────────────────┘
```

---

## Key Invariants

### 1. `ClassId` encodes the package/class boundary precisely
- `ClassId(FqName("a"), FqName("b"))` = class `b` in package `a`
- `ClassId(FqName.ROOT, FqName("a.b"))` = nested class `a.b` in root package
- **The same string `"a.b"` maps to DIFFERENT ClassIds depending on interpretation**

### 2. `resolve()` returns `ClassId?` — this is the primary resolution path
When `resolve()` succeeds, FIR uses the returned ClassId directly. No ambiguity.

### 3. `findClassId()` is the fallback — it probes longest-package-first
When `resolve()` returns null, FIR falls back to `findClassId(name)` which tries all splits. For `"a.b"` this means `ClassId("a","b")` is tried BEFORE `ClassId("","a.b")`. This order violates JLS 6.5.2 for edge cases — which is why `resolve()` must handle those cases.

### 4. `classifierQualifiedName` is informational, NOT used for resolution
FIR calls `resolve()` for ClassId lookup. `classifierQualifiedName` is only used for diagnostics and display. Don't confuse the two.

---

## Resolution Priority in JavaResolutionContext (JLS-compliant)

For simple names:
1. **Explicit (single-type) imports** — `import a.b.C;` → C maps to `a.b.C`
2. **Same package** — class in same package as the source file
3. **java.lang** — implicit import of `java.lang.*`
4. **Star imports** — `import a.b.*;` → tries `a.b.C`
5. **Supertype-inherited nested classes** — from containing class hierarchy

For dotted names like `A.B`:
1. Try resolving `A` as a class (via simple name rules above)
2. If `A` resolves to a class, try `A.B` as nested class → returns `ClassId(pkg_of_A, FqName("A.B"))`
3. If `A` doesn't resolve as a class, falls through to FIR's `findClassId` which probes all splits

---

## Common Mistakes and How to Avoid Them

### Mistake: Changing `findClassId` order
`findClassId` is shared with PSI. Changing its probe order breaks PSI tests. Fix resolution in `JavaResolutionContext.resolve()` instead — it only affects java-direct.

### Mistake: Returning a string from resolve and expecting FIR to split it correctly
String `"a.b"` is inherently ambiguous. Always return `ClassId` from `resolve()`.

### Mistake: Fixing resolution at the wrong layer
- **AST layer** (`rawTypeName`): only for extracting text from syntax nodes
- **Resolution layer** (`JavaResolutionContext`): for import/scope/JLS rules
- **FIR layer** (`JavaTypeConversion`): for ClassId probing and symbol lookup
- If the problem is "wrong class found for a qualified name" → fix is in Resolution layer
- If the problem is "annotation text in type name" → fix is in AST layer
- If the problem is "shared code creates wrong ClassId for resolved name" → fix is in FIR layer, but check upstream first

### Mistake: Confusing "resolution didn't find it" with "resolution found the wrong thing"
These require different fixes:
- **Not found**: Add a new resolution path (e.g., same-package lookup for root package)
- **Wrong thing found**: Fix priority order or add disambiguation (e.g., `resolveToClassId`)

---

## Annotation Resolution Pipeline (separate from type resolution)

```
Annotation source: "@NotNull"
      │
      ▼
┌─────────────────────────────────────┐
│ JavaAnnotationOverAst               │
│  classId: lazy, from resolveAnnotation()
│  resolveAnnotation(tryResolve) uses │
│  JavaResolutionContext.resolve()    │
│  Same import/scope rules as types   │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│ FIR: buildFirAnnotation()           │  javaAnnotationsMapping.kt
│  Calls annotation.resolveAnnotation │
│  callback = symbolProvider.get...   │
└─────────────────────────────────────┘
```

**TYPE_USE annotation filtering** happens at the Java Model level via `filterTypeUseAnnotations(isTypeUse)`. The callback checks annotation target in FIR. Critical: ALL annotation sources (modifier list, inline, extra) must be filtered — not just `extraAnnotations`.

---

*Created: 2026-03-19 (post iter-43 retrospective)*