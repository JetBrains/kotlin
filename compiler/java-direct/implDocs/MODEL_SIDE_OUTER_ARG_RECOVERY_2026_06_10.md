# Model-side outer-argument recovery (2026-06-10)

## Motivation

`JavaTypeConversion.kt` is a **shared FIR** file (PSI, binary, and `java-direct` all run through it).
Today it carries `java-direct`-specific code: the `outerTypeArgs` recovery branch in
`toConeKotlinTypeForFlexibleBound` plus three private helpers
(`findOuterTypeArgsFromHierarchy`, `findTypeArgsForClassInHierarchy`, `substituteTypeArgs`) and a
side-channel (`MutableJavaTypeParameterStack.containingClassSymbol`, set in
`FirJavaFacade.convertJavaClassToFir`).

That code reconstructs the JLS-implicit outer-class type arguments for **bare inherited
inner-class references** that the `FirBackedJavaClassAdapter` cannot supply. Concretely, for

```java
abstract class SuperClass<T> { class NestedInSuperClass { void nested(T x) {} } }
class KFirst extends SuperClass<String> {}
public class J1 extends KFirst {
    public class NestedSubClass extends NestedInSuperClass {}   // bare reference
}
```

`extends NestedInSuperClass` really means `extends SuperClass<String>.NestedInSuperClass`; the
`<String>` lives only in the resolved supertype chain `J1 → KFirst → SuperClass<String>`.

The goal of this change is to **relocate** that recovery out of the shared FIR file and into the
`java-direct` model, so `JavaTypeConversion.kt` returns toward its upstream shape, and to remove
the `containingClassSymbol` side-channel entirely.

## The PSI parallel

PSI gets these implicit outer arguments "for free" at the FIR conversion site because they were
already computed one layer earlier, inside the IntelliJ Java resolver via a synthetic light class:

- `FirJavaElementFinder.buildStub` writes the canonical generic supertype string
  (`extends SuperClass<java.lang.String>`) into the light `PsiClass`'s `EXTENDS_LIST`, resolving
  supertypes either from already-resolved `superTypeRefs` or, when not yet resolved, **on-air**:

  ```kotlin
  private fun FirRegularClass.resolveSupertypesOnAir(session: FirSession): List<FirTypeRef> {
      val visitor = FirSupertypeResolverVisitor(session, SupertypeComputationSession(), ScopeSession())
      return visitor.withFile(session.firProvider.getFirClassifierContainerFile(this.symbol)) {
          visitor.resolveSpecificClassLikeSupertypes(this, superTypeRefs, resolveRecursively = true)
      }
  }
  ```

- `PsiSubstitutor` (from `psi.resolveGenerics()`) then performs the inheritance-substitutor walk
  at the reference site, deriving `T → String`.

`FirBackedJavaClassAdapter` is the `java-direct` analog of that light class, but a deliberately
*thin* one: it carried an outer-class chain + type *parameters*, but `supertypes = emptyList()`,
so the implicit outer *arguments* were missing — which is exactly why the FIR-side recovery
existed. This change makes the adapter behave like the light class (real supertypes) and moves the
substitutor walk into the model's `computeTypeArguments`.

## New / changed components

1. **`FirBackedJavaClassAdapter.supertypes`** (was `emptyList()`): resolves the real FIR supertype
   chain, mirroring `FirJavaElementFinder.resolveSupertypesOnAir` — prefer already-resolved
   `superTypeRefs`; otherwise resolve in a throwaway
   `FirSupertypeResolverVisitor(session, SupertypeComputationSession(), ScopeSession())`. Each
   resulting `ConeClassLikeType` is wrapped as a model-private `FirBackedJavaClassifierType`.

2. **`FirBackedJavaClassifierType`** (new, model-private in `model/JavaTypeOverAst.kt`): a
   `JavaClassifierType` backed by a `ConeClassLikeType`. Its `classifier` is a
   `FirBackedJavaClassAdapter` for the cone's `classId`; its `typeArguments` are model-private
   `JavaType` wrappers (`FirBackedJavaType`) that convert the cone projections back so FIR's
   `toConeKotlinTypeForFlexibleBound` reproduces the originals (concrete classes, star projections,
   type-parameter references, nullability). **No public Java-model interface member is added**
   (rule 7) — these are `internal` model classes.

3. **`JavaClassifierTypeOverAst.computeTypeArguments`** (model): keeps the explicit-args and
   same-file `findTypeParameter` paths; adds the **inherited** case. For a non-static inner
   classifier whose outer args are not lexically in scope, it reads
   `resolutionContext.scopeContext.containingClass`, walks its **outer** chain through the
   adapter's new `supertypes`, locates the target outer class, and substitutes type arguments down
   the chain (`A<X> : Super<X>`, `A<String> ⇒ Super<String>`). The recovered args are appended as
   FIR-backed `JavaType`s so FIR reconstructs `SuperClass<String>.NestedInSuperClass`.

## FIR deletions

- `JavaTypeConversion.kt`: the `outerTypeArgs` computation block, its two `mappedTypeArguments`
  cases, and the three helpers (`findOuterTypeArgsFromHierarchy`,
  `findTypeArgsForClassInHierarchy`, `substituteTypeArgs`); `mappedTypeArguments` returns to its
  upstream shape.
- `MutableJavaTypeParameterStack.kt`: the `containingClassSymbol` field + its `copy()` propagation
  + KDoc.
- `FirJavaFacade.convertJavaClassToFir`: the `javaTypeParameterStack.containingClassSymbol = classSymbol`
  setter.

## Cycle safety

The recovery moves on-air supertype resolution into the type-conversion phase, which is the
primary risk. Mitigations (all routed through `JavaModelSessionAccess`, which uses the term
*cycle*, not *loop*):

- **Prefer already-resolved data.** Use `superTypeRefs` directly when
  `all { it is FirResolvedTypeRef }`; only fall back to on-air resolution otherwise.
- **`cycleSafeClassLikeSymbol`** funnels every symbol-provider lookup, breaking the KT-74097
  PUBLICATION-lazy cycle (`JavaModelInFlightResolutions`).
- **`cycleGuardedSupertypeWalk(resolvedClassId, default = emptyList())`** wraps the supertype walk,
  bounding direct/indirect Java inheritance cycles (`JavaModelSupertypeWalkGuard`).
- **Skip the in-flight class.** The model-side recovery walks only the containing class's *outer*
  classes (whose supertypes are already resolved), never re-entering the in-flight class's own
  supertype resolution — the same invariant the deleted `findOuterTypeArgsFromHierarchy` enforced.
- **No symbol provider ⇒ empty.** Parsing-level fixtures (`classifierAdapterFor` returns `null`)
  yield `supertypes = emptyList()` without throwing.

`JavaCycleBreakerTest` (direct `A extends A`, indirect `A → B → A`, KT-74097 PUBLICATION-lazy
probe) is the trip-wire and must stay green.

## Cone → Java → cone round-trip risk

Recovered args pass back through FIR conversion as `JavaType`s. Wildcards, star projections,
type-parameter references, and nullability must reproduce the original cone projections exactly,
or golden output diverges. `FirBackedJavaType`/`FirBackedJavaClassifierType` must cover these
cases; `KJKComplexHierarchyWithNested.fir.txt` and
`generics/innerClasses/j+k_complex.kt` are the golden trip-wires (executed by both the PSI runner
and the `java-direct` phased runner).

## Validation gates

- `:compiler:java-direct:test` — `JavaUsingAstPhasedTestGenerated` + `JavaUsingAstBoxTestGenerated`.
- `:compiler:java-direct:test` — `JavaCycleBreakerTest` + `JavaParsingTest`.
- PSI gate — `PhasedJvmDiagnosticLightTreeTestGenerated.*`.
- Cross-module gate — `*FirLightTreeBlackBoxCodegenTestGenerated*CompileKotlinAgainstKotlin*`.

Any net regression in either shared-FIR gate ⇒ revert (rule 6: never edit shared test data to make
`java-direct` pass).
