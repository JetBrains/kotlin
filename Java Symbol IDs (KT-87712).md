# Java Symbol IDs ([KT-87712](https://youtrack.jetbrains.com/issue/KT-87712))

**Author:** [Marco Pennekamp](mailto:marco.pennekamp@jetbrains.com)
**Parent:** [KT-84343](https://youtrack.jetbrains.com/issue/KT-84343) "FIR as Data", [KT-70517](https://youtrack.jetbrains.com/issue/KT-70517) "Back references to FIR"
**Branch:** `pennekamp/aa/fir-as-data`

D R A F T — analysis & work breakdown

This document analyses the state of Java symbols with respect to source-based symbol IDs and breaks the work
down into three categories:

1. [Unenhanced Java symbols](#1-unenhanced-java-symbols) — the `FirJavaClass` tree cached by Java symbol providers.
2. [Enhanced Java symbols](#2-enhanced-java-symbols) — the mirror tree cached by `FirEnhancedSymbolsStorage`.
3. [Other related symbols](#3-other-related-symbols) — renamed-for-override copies, synthetic properties, and other
   edge cases.

Categories 1 and 2 are both required: measurements show the memory contribution of unenhanced vs. enhanced Java
symbols to be roughly 50/50, and — more importantly — **the same Java PSI is referenced from both**, so covering only
one of them would not reduce the retained PSI, which is currently the single biggest memory contributor.

All code locations in this document refer to the state of the branch at the time of writing.

## Current state

Every Java FIR symbol currently uses the no-arg symbol constructor, i.e. a `FirUniqueSymbolId`. The affected creation
sites are marked with `// TODO (marco): Java symbol IDs.` and are listed per category below.

Java FIR is cached in **two tiers**:

* `LLFirJavaSymbolProvider` — `JavaSymbolProvider.classCache` (by `ClassId`) plus
  `LLPsiAwareClassLikeSymbolCache` (by `PsiClass`, for `ClassId` ambiguities). One entry per Java class,
  **including nested classes**.
* `FirEnhancedSymbolsStorage.cacheByOwner` — the enhanced mirror, keyed by the owner `FirRegularClassSymbol` and then
  by the original member symbol.

Java FIR is currently **not covered by any consistency check**:

* `collectCachedCheckableRootDeclarations` (`declarations/roots/utils.kt`) only collects FIR files and
  `LLKotlinStubBasedLibrarySymbolProvider.cachedTopLevelDeclarations`.
* `LLFirJavaSymbolProvider.cachedDeclarations` exists but is only consumed by `LLSessionStatisticsCalculator`.
* `FirEnhancedSymbolsStorage` has no enumerable view at all.
* The compiler-side `FirDistinctSourceElementsHandler` only walks FIR files.

Consequently there is no empirical baseline for Java distinctness. **Establishing one is the first work item**
(see [Checks & verification](#checks--verification)).

## Design implications for "FIR as Data"

The Java analysis surfaces three points that need to be folded back into the design document.

NOTE: The source of truth for the design document is Google Docs. The markdown copy of it might or might not be in the project.

### Source elements are not always a sufficient anchor

`FIR as Data.md` currently frames symbol ID equality as reducing to source element equality, and prescribes
"introduce additional fake source element kinds" as the remedy for collisions. Java disproves the general form of
this: several *legitimately distinct* FIR declarations share one Java source element (see
[the collision inventory](#collision-inventory-one-psimethod-many-fir-declarations)), and some of them are
distinguished by data that a fixed `KtFakeSourceElementKind` object cannot carry (a `Name`, another symbol, a
session).

The resolution is that **additional symbol ID variants are the go-to mechanism whenever more information than the
PSI/source element is needed**. The onus is on the symbol ID to satisfy the equality contract, not on the caches to
work around it. Concretely, the design document should gain:

* A statement that source-based symbol IDs are one *family*, not the only non-unique kind, and that new variants may
  key on additional discriminators.
* Guidance on choosing between (a) a fake source element kind, (b) a dedicated symbol ID variant, and (c) a unique
  symbol ID. Rough rule of thumb from the Java analysis:
  * Use a **fake source element kind** when the discriminator is a fixed, enumerable role (e.g. "the renamed-for-override
    copy of this method" *if* the name can be encoded — see category 3).
  * Use a **dedicated symbol ID variant** when the discriminator is data (a session, a `Name`, another symbol ID).
  * Use a **unique symbol ID** when the declaration is not stored in a discardable cache and does not need
    cross-instance equality.
* A note that introducing a fake source element purely to disambiguate an *enhanced* declaration from its unenhanced
  original is **not** acceptable: `source`/`realPsi` is observable through `KaSymbol.psi`, navigation and diagnostics,
  and it would in any case be insufficient because enhancement is per use-site session.

### Cross-session symbol ID equality is not acceptable

Java member enhancement happens in the **use-site session**, not the declaring session
(`JavaScopeProvider.getUseSiteMemberScope` → `buildJavaEnhancementScope(useSiteSession, …)` →
`JavaClassMembersEnhancementScope` → `FirSignatureEnhancement(owner.fir as FirJavaClass, session)`). The comment in
`FirSignatureEnhancement` claiming that `owner` was created for the same session as the constructor argument only
holds for module data; in LL FIR the sessions genuinely differ.

Therefore one `PsiMethod` in a library yields **one enhanced symbol per use-site session**. Since
`LLRealPsiSymbolId.equals` deliberately ignores the session, purely PSI-derived IDs would make all of these equal.
That is not acceptable, so **the symbol ID of an enhanced Java symbol must key on the enhancing session** in
addition to the PSI.

Note that this also affects the checkers: `collectCheckableRootDeclarations` flattens roots from several modules into
one list, so the distinctness check will see cross-session declarations side by side.

> **Related, noted for later:** the same question applies to library symbol providers in general — a jar that belongs
> to two `KaLibraryModule`s would produce two sessions over the same PSI. Tracked separately.

### The Java cache root model needs correcting

`FIR as Data.md` says: "For Java declarations, the cache roots are the individual top-level FIR declarations cached
by Java symbol providers." Two corrections:

* Roots are **every Java class, nested classes included**. `classCache` is keyed by `ClassId` including nested
  classes, and `FirLazyJavaDeclarationList.declarations` does *not* contain nested classes (nested lookup goes
  through `existingNestedClassifierNames` / `lazyNestedClassifierScope`). This is convenient: a visitor over a
  `FirJavaClass` root stays within one cache entry, and Constraint 1 is not endangered by nested classes.
* There is a **second tier of Java cache roots** that the document does not mention at all: `FirEnhancedSymbolsStorage`,
  `FirRenamedForOverrideSymbolsStorage`, and the scope-session-local cache in
  `JavaAnnotationSyntheticPropertiesScope`. Each enhanced member is its own root (it is not part of the
  `FirJavaClass` tree), and needs its own root/back-reference story.

### Collision inventory: one `PsiMethod`, many FIR declarations

For reference, the FIR declarations that can simultaneously exist for a single Java method, all sharing its source
element:

| # | Declaration | Created at | Category |
|---|---|---|---|
| 1 | unenhanced `FirJavaMethod` | `FirJavaFacade.convertJavaMethodToFir` | 1 |
| 2 | enhanced `FirNamedFunction` (same `callableId`, same symbol class) | `SignatureEnhancement.enhanceMethod` | 2 |
| 3 | `FirIntersectionOverrideFunctionSymbol` | `SignatureEnhancement.enhanceMethod` | 2 |
| 4 | renamed-for-override copy (parameterized by natural `Name`) | `JavaClassUseSiteMemberScope.createCopyWithNaturalName` | 3 |
| 5 | hidden-signature-clash / accidental-override / declared-copy-with-supertype-parameter-types copies | `JavaClassUseSiteMemberScope` | 3 |
| 6 | value/type/receiver parameters of each of the above | see per-category sections | 1/2/3 |

Because 1 and 2 are both `FirNamedFunctionSymbol`, the sanity check in `FirBasedSymbol.equals` (equal symbol IDs must
imply the same symbol class) would *not* catch their conflation. It would be silent.

## 1. Unenhanced Java symbols

**Scope:** the `FirJavaClass` tree as produced by `FirJavaFacade` and cached by `LLFirJavaSymbolProvider` /
`LLJvmClassFileBasedSymbolProvider`.

**Good news up front:** the raw Java conversion appears to be free of source element collisions *within* a class
root. All fake kinds used inside one `FirJavaClass` are already disambiguated
(`JavaRecordComponentFunction`, `ImplicitRecordConstructorParameter`, `ImplicitJavaRecordConstructor`,
`ImplicitJavaAnnotationConstructor`, `ImplicitConstructor`, plus real `PsiField`/`PsiMethod`/`PsiParameter`/
`PsiTypeParameter` sources), and the annotation-class vs. regular-class constructor paths are mutually exclusive.
Type parameter bounds are enhanced in place via `replaceBounds`, so enhancement adds no declarations there. This
means category 1 is largely mechanical, and it is the right first prototype step.

### 1.1 Symbol ID creation

Replace unique symbol IDs at the following sites with `session.symbolIdFactory.sourceBasedOrUnique(source)`:

| Location | Declaration |
|---|---|
| `JavaSymbolProvider.kt:45` | `FirRegularClassSymbol` for a `FirJavaClass` (`ClassId` cache) |
| `symbolProviders/utils.kt:90` (`createPsiClassSymbol`) | `FirRegularClassSymbol` (PSI-ambiguity cache and Standalone path) |
| `FirJavaFacade.kt:404` (`toFirTypeParameter`) | type parameters of classes, methods and constructors |
| `FirJavaFacade.kt:432` | record component function |
| `FirJavaFacade.kt:462` | implicit canonical record constructor |
| `FirJavaFacade.kt:509` | enum entry |
| `FirJavaFacade.kt:534` | field |
| `FirJavaFacade.kt:580` | method |
| `FirJavaFacade.kt:653` | constructor (declared and implicit) |
| `FirJavaFacade.kt:717` | annotation class constructor |
| `FirJavaValueParameter.kt:245` | every `FirJavaValueParameter` |

Notes:

* The class symbol is created *before* the FIR class, in the symbol provider. Both provider sites have the
  `JavaClass`/`PsiClass` in hand at that point, so the source element is available
  (`JavaElement.toSourceElement()` / `psiClass`).
* `FirJavaValueParameterBuilder.build()` has no session, but it has `moduleData`, so
  `moduleData.session.symbolIdFactory` works. The same holds for the free functions in `FirJavaFacade.kt`.
* `JavaElement.toSourceElement()` returns `null` for `JavaClass` implementations that are not `JavaElementImpl<*>`
  (i.e. non-PSI, binary Java classes). This is expected outside AA+IDE — including Standalone — and
  `sourceBasedOrUnique` degrading to a unique symbol ID there is the intended behaviour. The constraint checks will
  flag it if it ever happens in a configuration where it shouldn't.

### 1.2 Restoration

`LLRealPsiSymbolId.isSupported` is currently `this is KtDeclaration || this is KtFile`, and the check runs in
`init`. **Flipping `JavaSymbolProvider.kt:45` without touching this will throw immediately on every Java class.**

* **`PsiClass`:** straightforward. `LLResolutionFacade.resolveToFirSymbol(psiClass)` already exists and routes
  through `getClassLikeSymbolByPsiWithoutDependencies`, which is exactly the right entry point (it also handles the
  `ClassId` ambiguity case). Add `PsiClass` to `isSupported` and to the `when` in `resolveSymbol`.
* **Java members** (`PsiMethod`, `PsiField`, `PsiParameter`, `PsiTypeParameter`, `PsiRecordComponent`): there is no
  `resolveToFirSymbol` equivalent. It needs to be built: restore the containing `PsiClass`, force
  `FirJavaClass.declarationList.declarations`, and match by PSI identity. **Forcing the whole lazy declaration list
  to restore one member is accepted for now.**
* Until member restoration exists, members can use non-restorable, strongly-referencing source-based IDs (the
  `LLNonRestorableRealPsiSymbolId` shape). This already yields source-based *equality*, which is what the back-references
  prototype needs; only the memory benefit is deferred. Compare the third symbol ID category sketched in the
  `FirSymbolIdFactory` TODO.

### 1.3 Root declarations / back references

The stub-library pattern of assigning back references once at root creation
(`StubBasedClassDeserialization.kt`, `assignRootDeclarationReferences(this, this)`) does **not** transfer directly:

* `FirLazyJavaDeclarationList.declarations` is `by lazy(LazyThreadSafetyMode.PUBLICATION)`, so at
  `convertJavaClassToFir` time only the class and its type parameters exist.
* Back references for members must therefore be assigned from *inside* the lazy block (or via
  `assignRootDeclarationReferencesFrom` once the list is materialised).
* `PUBLICATION` also means the declaration list can be computed more than once concurrently, with the loser
  discarded. Each attempt builds its own declarations and its own symbol IDs, so no symbol ID instance is shared;
  only the surviving list is reachable from the root. The distinctness check is unaffected, but the transient
  duplication is worth keeping in mind when reading session statistics.

### 1.4 Connectivity

A nested `FirJavaClass` strongly references its parent root via `containingClassSymbol`,
`FirOuterClassTypeParameterRef`s pointing at the parent's `FirTypeParameterSymbol`s, and a copy of the parent's
`classJavaTypeParameterStack`. **This is acceptable** — a whole top-level class tree keeping itself alive is the
expected granularity. No action needed in this category.

## 2. Enhanced Java symbols

**Scope:** everything produced by `FirSignatureEnhancement` and cached in `FirEnhancedSymbolsStorage`.

This is the harder half. The enhanced declaration copies the original's `source` verbatim and keeps the original's
`callableId`, so it is indistinguishable from the unenhanced original by source element alone — and enhancement is
per use-site session on top of that.

### 2.1 Symbol ID design (the key decision)

An enhanced Java symbol needs a **new symbol ID variant** keyed on at least:

* the PSI element (or the original symbol's symbol ID), and
* the **enhancing session** (see [Cross-session symbol ID equality](#cross-session-symbol-id-equality-is-not-acceptable)), and
* a marker distinguishing "enhanced" from "unenhanced".

Sketch of the two shapes worth evaluating:

* `LLEnhancedJavaSymbolId(base: FirSymbolId<*>, session: LLFirSession)` — a *derived* symbol ID that delegates to the
  unenhanced symbol's ID for the PSI anchor and adds the session plus its own type as discriminators. Composes
  naturally with member restoration (restore the base, then re-enhance).
* A flatter `LLEnhancedJavaSymbolId(psi: PsiElement, session: LLFirSession)`.

The derived shape is probably preferable because it makes the enhanced/unenhanced relationship explicit and reuses
whatever anchor category 1 settles on. Either way, `source` must be left untouched.

Also to decide: whether intersection overrides created during enhancement
(`SignatureEnhancement.kt:424`, currently `FirIntersectionOverrideFunctionSymbol(FirUniqueSymbolId(), …)`) get the same
treatment or stay unique. They are stored in `FirIntersectionOverrideStorage` rather than the enhancement cache, so
they may be deferrable — but they hold the overridden symbols in a list, and their identity feeds
`FirTypeIntersectionScopeContext` caches (already converted to symbol ID keys), so verify before deferring.

### 2.2 Symbol ID creation

| Location | Declaration |
|---|---|
| `SignatureEnhancement.kt:180` | enhanced `FirFieldSymbol` |
| `SignatureEnhancement.kt:371` | enhanced `FirConstructorSymbol` |
| `SignatureEnhancement.kt:432` | enhanced `FirNamedFunctionSymbol` |
| `SignatureEnhancement.kt:424` | `FirIntersectionOverrideFunctionSymbol` (decide: enhanced variant or unique) |
| `SignatureEnhancement.kt:460` | receiver parameter of an enhanced function |
| `SignatureEnhancement.kt:574` | value parameters of an enhanced function (`buildEnhancedValueParameter`) |
| `SignatureEnhancement.kt:595` | type parameter copies (`copyTypeParametersWithNewContainingDeclaration`) |
| `SignatureEnhancement.kt:220, 222, 225` | enhanced synthetic property + its accessors (see category 3) |

The parameters and type parameters **must** move together with their owner: if the enhanced function becomes a
non-unique root while its parameters stay unique, Constraint 1 is violated directly.

### 2.3 Restoration — deferred

Restoring an enhanced symbol from PSI is not currently possible even in principle. It would require knowing the
enhancing session, rebuilding `JavaClassMembersEnhancementScope` for it, and knowing the `name` and
`precomputedOverridden` that were used at creation (`FirEnhancedSymbolsStorage.FunctionEnhancementContext`) — none of
which is derivable from the PSI element.

**Decision: leave enhanced symbol restoration as a follow-up.** The enhancement architecture should probably be
cleaned up first; restoration will be much easier afterwards. Until then, enhanced symbol IDs reference their symbol
strongly.

### 2.4 Enhanced → unenhanced edges

This is the one connectivity problem in the Java area that does need work. An enhanced declaration strongly retains
the unenhanced tree through:

* `attributes.copy()` (including `initialSignatureAttr`, which holds a symbol),
* `FirDelegatedJavaAnnotationList(firElement)` for fields,
* shared `defaultValue` expressions (`buildEnhancedValueParameter` mutates and reuses the original's default value),
* `overriddenMembers` lists,
* `dispatchReceiverType`.

Plus `FirEnhancedSymbolsStorage.cacheByOwner` strongly keys on the owner `FirRegularClassSymbol`, pinning the Java
class root for as long as the enhancement cache lives.

Until these are converted to symbol IDs, making the Java class root discardable buys nothing. This overlaps with
[KT-70701](https://youtrack.jetbrains.com/issue/KT-70701) and is **in scope for this task**, not a follow-up.

### 2.5 Root declarations / back references

Each enhanced member is its own cache root. Back references have to be assigned where the enhanced declaration is
created (inside the `FirEnhancedSymbolsStorage` cache computation), pointing at the enhanced declaration itself.
Note that `enhancedFunctions`' post-processing step re-enters enhancement for `initialSignatureAttr`, so that path
needs covering too.

## 3. Other related symbols

These are the remaining Java-adjacent symbol families. They are all stored *outside* the `FirJavaClass` tree, so
Constraint 1 is formally satisfied even if they stay unique. The question for each is whether its cache is
discardable and whether it needs cross-instance equality.

**Guiding principle from the discussion:** prefer a fake source element kind where the discriminator is expressible
as one, otherwise a unique symbol ID. Several of these have questionable lifetime characteristics already, so
deferring them is reasonable.

### 3.1 Renamed-for-override family

`FirRenamedForOverrideSymbolsStorage` (session component) caches four kinds of copies, all keyed by
`Pair<FirNamedFunctionSymbol, Name>` and all copying the original's `source`:

| Location | Copy |
|---|---|
| `JavaClassUseSiteMemberScope.kt:1052` | `createCopyWithNaturalName` (renamed for override; also used for hidden copies) |
| `JavaClassUseSiteMemberScope.kt:1097` | `createDeclaredFunctionCopyWithParameterTypesFromSupertype` |
| `JavaClassUseSiteMemberScope.kt:1121` | `createAccidentalOverrideWithDeclaredFunctionHiddenIfDeclaredFunctionParametersAreErasedCopy` |
| `JavaClassUseSiteMemberScope.kt:1137` | `createAccidentalOverrideWithDeclaredFunctionHiddenIfInheritedFunctionParametersAreErased…` |
| `JavaClassUseSiteMemberScope.kt:1105` | `buildJavaValueParameterCopy` for the copied parameters |

The renamed copy's discriminator is the natural `Name`, which **could** be encoded in a fake source element kind
that carries the name (a parameterized fake kind rather than an object). That is the preferred route if a
parameterized fake kind is acceptable; otherwise a dedicated symbol ID variant.

Because these caches are keyed by symbol, they are equality-sensitive: whatever category 1 and 2 do must not make a
copy equal to its original, or the cache will conflate them.

The suspend-function conversion copy at `JavaClassUseSiteMemberScope.kt:424` is *not* cached (there is already a TODO
there); a unique symbol ID is probably fine, but confirm it never needs cross-instance equality.

### 3.2 Synthetic properties for Java getters

* `SignatureEnhancement.kt:220, 222, 225` — `FirJavaOverriddenSyntheticPropertySymbol` and two
  `FirSyntheticPropertyAccessorSymbol`s, built by `buildSyntheticProperty` **without a source element at all**.
  So `sourceBasedOrUnique(null)` degrades to unique here unless an anchor is derived from the delegate getter.
* `JavaAnnotationSyntheticPropertiesScope.kt:53, 59` — same symbols, cached in a **scope-session-local**
  `hashMapOf`. There is already a TODO asking whether unique IDs are acceptable here; the answer depends on the
  scope session's lifetime relative to the FIR it references. Compare `JavaClassUseSiteMemberScope`, whose synthetic
  property cache lives in the session.

### 3.3 Symbol copying

`FirSyntheticPropertySymbol.copy()` passes the **same** `symbolId` instance to the new symbol
(`FirJavaOverriddenSyntheticPropertySymbol.kt:54`). Since `FirBasedSymbol.init` calls `symbolId.bind(this)`, the copy
silently steals the original's binding, so `symbolId.symbol` on the original now returns the copy.

There are already TODOs on this (`FirFunctionSymbol.kt`, `FirJavaOverriddenSyntheticPropertySymbol.kt`) raising the
right question: does a copy need a symbol ID that is *distinct* from the original's, and does `copy` even make sense
for non-unique symbol IDs? Synthetic properties are a Java-only construct, so this lands in the Java work — but it is
cursed enough to be **ignored for the prototype**.

## Checks & verification

**Do this first, before changing any symbol creation.** It converts the static analysis in this document into an
empirical inventory and gives a regression net for every later step.

1. Add `LLFirJavaSymbolProvider.cachedDeclarations` (and the `LLJvmClassFileBasedSymbolProvider` equivalent) to
   `LLFirSession.collectCachedCheckableRootDeclarations`. The `@FirCacheInternals` accessor already exists and
   already covers both the main and the ambiguity cache.
2. Add an enumerable view of `FirEnhancedSymbolsStorage` (and, later, of `FirRenamedForOverrideSymbolsStorage`) and
   feed it into the same collection, as a separate tier of roots.
3. Decide whether the checkers force `FirJavaClass.declarationList.declarations`. Forcing gives real coverage at the
   cost of heavier tests; not forcing only checks what a test happened to materialise. Forcing is probably right for
   the distinctness and Constraint 1 checks.
4. Expect the distinctness check to report **cross-session** duplicates as soon as Java is wired in — that is the
   symptom of the issue described in
   [Cross-session symbol ID equality](#cross-session-symbol-id-equality-is-not-acceptable), not a checker bug.
5. Once category 1 is done, `checkRootDeclarationReferences` should be extended to Java roots, keeping the lazy
   declaration list caveat from §1.3 in mind.

For effectiveness (rather than correctness), the relevant metric is retained **Java PSI**, which is currently the
largest contributor. Memory snapshots and session structure logging ([KT-80622](https://youtrack.jetbrains.com/issue/KT-80622)) with
low-memory cache cleanup disabled are the measurement vehicles, as for the rest of the parent task.

## Suggested order of work

1. **Checkers for Java roots** (see above). No behaviour change, immediate diagnostic value.
2. **Category 1, symbol IDs + `PsiClass` restoration.** Members get non-restorable strong source-based IDs at first.
   Self-contained; should pass distinctness given the clean state of the raw conversion.
3. **Member `resolveToFirSymbol`** for Java members, then switch member IDs to restorable.
4. **Design the enhanced symbol ID variant** (§2.1) and write it into `FIR as Data.md` together with the general
   "additional symbol ID variants" guidance from
   [Design implications](#design-implications-for-fir-as-data).
5. **Category 2, symbol IDs**, including parameters and type parameters. Restoration deferred.
6. **Enhanced → unenhanced edge reduction** (§2.4) plus symbol ID keys in `FirEnhancedSymbolsStorage`. This is what
   actually unlocks discarding Java class roots.
7. **Category 3**, as needed and driven by what the checkers report.

## Further questions

* Can a fake source element kind be *parameterized* (e.g. carrying a `Name`) within the current
  `KtFakeSourceElementKind` design, or does every parameterized discriminator have to become a symbol ID variant?
  This decides the shape of §3.1.
  * Yes, it can.
* Does `FirIntersectionOverrideFunctionSymbol` created during Java enhancement need a non-unique ID, given that it is
  stored in `FirIntersectionOverrideStorage` and feeds symbol-ID-keyed scope caches?
  * Not yet, this is rather in the scope of migrating the intersection override storage.
* Should the enhanced symbol ID derive from the unenhanced symbol's ID (composable, explicit) or from the PSI
  directly (flatter, one less indirection)?
  * The former seems more elegant.
* What is the right anchor for synthetic property symbols, which have no source element of their own — the delegate
  getter's symbol ID, or unique?
* `FirSymbolId.copy` semantics: is a copy required to be distinct from its original even for source-based IDs? (See
  the existing TODO in `FirFunctionSymbol.kt`.)
