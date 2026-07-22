# Java-Direct: Performance Review — 2026-07-20

**Status**: Analysis complete. Low-risk fixes **landed** (full box+phased suite green,
2767/2767, 0 failed). Riskier candidates are listed for follow-up, not applied.

**Scope reviewed**: the Java-source model (`model/*.kt`), the classpath/source indexers
(`JvmBinaryClassFinderInputsOverIndex.kt`, `JavaPackageIndexer.kt`, `JavaPackageInfoIndexer.kt`,
`util/JavaSourceIndex.kt`), the supertype graph (`util/JavaSupertypeGraph.kt`), and the
session/resolution caches (`JavaModelSessionAccess.kt`, `JavaResolutionContext.kt`,
`resolution/*`).

**Headline**: the module is already in decent shape — the session-level guards/caches are
sound. The one systemic inefficiency was the **model layer recomputing derived values on
every access**. Instances of `JavaClassOverAst` are cached per `ClassId` (via `JavaClassCache`),
and, after the fixes below, the member/type wrappers they hand out are also stable, so
per-instance memoization is now effective end-to-end.

---

## Part 1 — Applied fixes (non-controversial: memoize recomputed reads)

All are `by lazy(LazyThreadSafetyMode.PUBLICATION)`, matching the existing precedent for
`JavaClassOverAst.supertypes` and `JavaMethodBaseOverAst.typeParameters`. `PUBLICATION` keeps
these safe under FIR's concurrent resolution and safe against the KT-74097 re-entrance guard
(a re-entrant read may compute twice but the value is idempotent). Each converted getter is a
**pure function of the immutable AST** (plus the already-lazy `classifier`), so caching the
first result is behaviour-preserving.

### `model/JavaClassOverAst.kt`
| Property | Was | Why it mattered |
|---|---|---|
| `isInterface`, `isAnnotationType`, `isEnum`, `isRecord`, `isSealed` | `get()` doing a `findChildByType`/modifier scan each call | Read very frequently (e.g. per-member `visibility`, every `isStatic`, supertype building). Each read re-scanned the class node's children — effectively O(children) per read, O(members × children) per class. |
| `methods`, `fields`, `constructors`, `recordComponents` | `get()` re-scanning children and **re-allocating fresh wrapper instances** each call | FIR forces these from multiple slots; every read rebuilt the list and produced *new* member objects, defeating any per-member memoization downstream. |
| `innerClassNames` | `get()` re-scanning + re-allocating `Name`s | Read by the supertype graph and FIR. |
| `annotations` | `get()` re-parsing the modifier list | Read for deprecation/nullability + `findAnnotation`. |

Memoizing `methods`/`fields`/etc. is the **key enabler**: because the returned member
instances are now stable, the per-member lazies below actually cache across the many reads FIR
does, instead of being thrown away with each freshly-rebuilt list.

### `model/JavaMemberOverAst.kt`
- `JavaMethodBaseOverAst.resolutionContext` — was re-forking the context
  (`memberResolutionContext.withTypeParameters(...)`) on every access; read by `returnType`,
  `valueParameters`, and `annotations`.
- `JavaMethodBaseOverAst.valueParameters` — re-scanned the parameter list and re-wrapped every
  parameter per access.
- `JavaMethodOverAst.returnType` — re-created the return-type wrapper per access.
- `JavaFieldOverAst.leadingFieldNode` — re-walked previous siblings (multi-field declarations)
  per access; read by `modifierList` and `type`.
- `JavaFieldOverAst.modifierList`, `type`, `initializerNode`, `annotations` — each recomputed
  a child walk / wrapper per access; read by the constant-initializer path (`hasInitializer`,
  `hasConstantNotNullInitializer`, `initializerValue`) and FIR.

### `model/JavaTypeOverAst.kt`
- `JavaClassifierTypeOverAst.rawTypeNameParts` — extracted from the AST reference node, fed up
  to four other reads (`rawTypeName`, classifier resolution, raw-ness, type-argument recovery).
- `JavaClassifierTypeOverAst.typeArguments` — re-collected reference-parameter lists and
  re-wrapped each argument type per access; read repeatedly during type conversion.

---

## Part 2 — Caches reviewed and found healthy (no change)

- **`JavaModelSessionAccess`** — the `(session, classId)` re-entrance guard and the
  builtins-filtered class-existence probe are correct and necessary (KT-74097). Concurrent,
  well-scoped.
- **`JavaSupertypeGraph`** — memoizes the direct-supertype `ClassId` lists with a concurrent
  map; the walk itself is bounded by `JavaSupertypeLoopChecker`. Data structure is appropriate.
- **`JavaPackageIndexer`** — `ConcurrentHashMap`-backed package/file/directory indices, built
  lazily per package. The two-phase design (cheap line-scan index now, full parse on demand)
  is intentional and not redundant.
- **`JavaClassOverAst.innerClassCache`** — positive-only `ConcurrentHashMap<Name, JavaClass>`;
  correct (identity contract for inner-class type parameters), negatives intentionally uncached.
- **`util/JavaSourceIndex.extractFileInfoLightweight`** — single line-by-line pass, no
  per-access recompute; fine.

---

## Part 3 — Proposed follow-ups (riskier / need investigation — NOT applied)

1. **`JvmBinaryClassFinderInputsOverIndex` uses plain `HashMap` for four caches**
   (`binaryCache`, `topLevelClassesCache`, `topLevelClassesCacheAllScope`,
   `knownClassNamesCache`), all populated via non-atomic `getOrPut`. This finder lives on the
   **library session**, which FIR can hit under concurrent resolution — a plain `HashMap` under
   concurrent `getOrPut` risks lost updates and, on resize, CPU-spinning corruption. **Why not a
   trivial fix**: three of these maps deliberately store `null` values (cache-the-miss), and
   `ConcurrentHashMap` forbids null keys/values, so a correct fix needs a null sentinel (or
   `Optional`) or a different structure. **Also verify reachability**: confirm whether the
   deserializer already serializes access to this finder before investing. Classify:
   *needs-investigation* (correctness/thread-safety more than throughput).

2. **`JavaAnnotationOverAst.classId` / `arguments` recomputation.** These recompute per access,
   but `classId` calls `resolve()`, which can legitimately return a *transient* `null` while a
   resolution cycle is in flight (the guard breaks the cycle by returning null for the in-flight
   id). Memoizing would risk pinning that transient null. Safe only if we memoize the *argument
   wrappers* (no resolution) while leaving `classId` resolution un-cached, or if we can prove
   `classId` is never first-read mid-cycle. Classify: *needs-investigation*.

3. **Remaining per-type getters** (`isRaw`, `rawTypeName`, `classifierQualifiedName`, and the
   base `typePositionAnnotations`/`annotations`) and **per-parameter** `type`/`annotations`.
   These are pure and could be memoized too, but `JavaClassifierTypeOverAst` / parameter objects
   are the **most numerous** model objects, so each added `lazy` field has a real memory cost.
   Recommend measuring before adding more lazies here; the two hottest (`rawTypeNameParts`,
   `typeArguments`) are already done. Classify: *needs-investigation (memory/throughput trade-off)*.

4. **`JavaMemberOverAst.name` allocates a `Name` per access.** Low individual cost, high call
   count. Candidate for memoization, but marginal; bundle with a measurement pass.

---

## Part 4 — Benchmarking recommendation

The applied fixes reduce **allocation and repeated AST scans on hot read paths**; the expected
effect is fewer allocations and less CPU on Java-heavy modules, with no behaviour change. To
quantify (and to gate any Part-3 work), use the existing harness:

- Corpus: `KotlinFullPipelineTestsGenerated` (414 modules, 109 with Java) for mixed workloads;
  `IntelliJFullPipelineTestsGenerated.testIntellij_platform_*` for Java-heavy workloads.
- Force the module on with `-Pfir.force.javaDirect=true`; measure warm-frontend wall+CPU over
  several iterations of the same build (see `INVESTIGATION_TECHNIQUES.md` → Performance
  Measurement Harness; the `phase-c-instrumentation` stash provides `ThreadMXBean` CPU brackets
  and per-classloader dumps).
- The known remaining gap vs the PSI model is small; treat Part-3 items as the next lever only
  if a measured hotspot points at them.

---

## Verification

- `:compiler:java-direct:test` — `JavaUsingAstBoxTestGenerated` + `JavaUsingAstPhasedTestGenerated`:
  **2767 tests, 0 failures / 0 errors / 0 skipped** (module main sources recompiled).
- Parser/model unit tests (`JavaParsing*`): **105 tests, 0 failures**.
- Changes confined to `compiler/java-direct/src/.../model/` — no shared-FIR file touched, so the
  PSI and `CompileKotlinAgainstKotlin` gates are not implicated (the box suite already exercises
  `CompileKotlinAgainstKotlin`).
