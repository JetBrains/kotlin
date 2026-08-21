# Raw-ness and Type Arguments, Computed Once (2026-08-27)

The second round of review threads on PR #7500 asked four questions about
`JavaClassifierTypeOverAst`. Two of them (1 and 3 below) turned out to be the same problem and share
one fix; the other two got a smaller change each. This note records what landed and why, and
supersedes §§4–5 of `IMPLICIT_OUTER_TYPE_ARGUMENTS_REVIEW_DECISIONS_2026_08_26.md`.

---

## 1 + 3. `null` is the representation of "no argument known"

**The threads.** *"We return the same type arguments for a properly defined type and for the case
where it actually should be a raw type."* And: *"it would be much easier to support the contract of
`JavaClassifierType#getTypeArguments`, which returns a list containing nulls as a sign that the type
is raw … currently we have separate logic written twice for very similar things."*

**The bug behind them.** `computeTypeArguments` had three outcomes and two of them were the same
list: every generic outer in scope, and no outer in scope with the recovery failing, both returned
the declaring class's own `JavaTypeParameter`s. The second is not a properly defined type — it is
the raw one. It is reachable through an inner class named by its simple name from a class that
neither encloses nor inherits it:

```java
package p;
import p.Outer.Inner;
public class Unrelated {
    public Inner viaImport() { return null; }        // javac: [rawtypes]
    public Outer.Inner viaQualified() { return null; } // javac: [rawtypes]
}
```

`viaQualified` was handled — `rawTypeNameParts.size > 1`, so the old `computeIsRaw` walked the outer
chain. `viaImport` was not: raw-ness was `false`, the argument was `Outer`'s own `T`, and since the
`MutableJavaTypeParameterStack` in play belongs to `Unrelated`, the lookup missed and the reference
became `ConeErrorType(ConeUnresolvedNameError)`. PSI answers `isRaw = true`, `typeArguments =
[null]` for the same source, so this was a divergence, not merely a lax answer.

**What landed.** `computeTypeArguments` emits one entry per type parameter the reference has to
supply — the classifier's own first, then the enclosing instances', innermost first, `static`
severing the chain, i.e. PSI's `getTypeParameters` order — and `null` wherever nothing is known.
`isRaw` is `typeArguments.any { it == null }`. `computeIsRaw` and `isQualifiedByInheritor` are gone.

| Reference | Entry | `isRaw` |
|---|---|---|
| `List` for `List<E>` | `null` | raw |
| `Inner<U>` inside `Outer<T>` | `Outer`'s own `T` | not raw |
| `Outer.Inner`, qualified by the declaring outer | `null` | raw — even inside `Outer`'s own body |
| `Sub.Inner`, `Sub extends Outer<String>` | recovered `String` | not raw |
| imported `Inner`, recovery fails | `null` | raw — the case that was wrong |

Two rules keep the answers identical to the old two-place logic apart from that last row: naming the
enclosing class at all (`rawTypeNameParts.size > 1`) opts out of implicit outer arguments, and naming
the *declaring* outer specifically skips the recovery, which is what `isQualifiedByInheritor` used to
express.

**One correction to the thread's premise.** The `null` convention does not make `isRaw` disappear.
Nothing downstream reads a `null` as "raw": `JavaTypeConversion` branches on `isRaw` and, when a type
is raw, discards `typeArguments` entirely; K1's `JavaTypeResolver.computeArguments` does the same; a
`null` in a non-raw type merely becomes `ConeStarProjection`. What the convention removes is the
duplication — and, structurally, the possibility of `isRaw` and `typeArguments` contradicting each
other, which is the shape of the bug that had already been fixed once on this branch.

**Tests.** `compiler/testData/diagnostics/tests/j+k/importedInnerClassOfGenericOuterIsRaw.kt`
(the reachable shape; observed through the type of `get()`, since both the raw and the error-type
answer silently accept anything passed to `put`), plus the model-level cases in
`JavaParsingImplicitOuterTypeArgumentsTest`.

---

## 2. `firBackedJavaType` takes a `declarationChainRoot`

**The thread.** *"May be it's worth putting this logic somehow to `firBackedJavaType`? Because
currently, for `ConeTypeParameterType` it silently returns `FirBackedJavaWildcardType`, which doesn't
make a lot of sense, while there are some other call-sites which might be affected too."*

The lookup itself cannot simply move: choosing the right `JavaTypeParameter` is contextual — it must
be the instance declared by the class whose supertype the argument was read off, or by one of its
outers, because FIR resolves a `JavaTypeParameter` only by identity in the per-class stack. But the
second half of the thread is right, and that half was the real defect: `firBackedJavaType` has two
call sites, and the other one is `FirBackedJavaClassifierType.typeArguments`. Only the *top-level*
recovered projections used to reach the declaration-chain lookup, so a residual parameter one level
down — recovering `Box<E>` rather than a bare `E` — degraded to `Box<*>`, invisibly, in the `else ->`
arm.

**What landed.** The context is threaded instead of the lookup being moved:
`firBackedJavaType(projection, session, declarationChainRoot = null)` now owns the
`lowerBoundIfFlexible()` unwrap and an explicit `is ConeTypeParameterType ->` arm, and
`FirBackedJavaClassifierType` carries the same root into its nested arguments.
`recoveredOuterTypeArgument` collapsed into a single call; `javaTypeParameterInDeclarationChain`
became `internal`. Behaviour for context-free uses is unchanged (an unbounded wildcard), but it is
now stated with its reason rather than reached by falling off the end of a `when`.

**Test.** `compiler/testData/diagnostics/tests/j+k/outerTypeArgumentRecoveredWithNestedTypeParameter.kt`
— `Mid<E> extends Outer<Box<E>>`, where `Box<*>` and `Box<E>` are distinguished by whether
`unwrap()` still returns a `String` for a `Mid<String>`.

---

## 4. The incremental test is now a pair

**The thread**, on `IncrementalJavaClassFromPreviousOutputTest`: *"Should a case without this
modification also be covered by a similar test?"*

At review time it would have been a weaker duplicate: with the `InnerClasses` attribute present the
classifier still resolved to `null`, and `JavaTypeConversion` re-split the dotted name into a package
that matches nothing, so both variants failed through the same production line. That stopped being
true with `41726707f45a` (KT-87507), which carries the recorded `ClassId` into `JavaTypeConversion`:
the attribute-present variant now resolves *without* a successful classpath-wide lookup, and the
stripped variant is the only one that exercises that lookup. The test body is now shared by two
`@Test`s, one per path, each documenting which path it is the only cover for.

**Not done, worth doing.** With `EverythingGlobalScope` the cross-reference may land on a stale
duplicate of the same class in the previous build's output directory rather than the library version.
Which one wins is currently unspecified; pinning it needs a variant where the two disagree, and a
decision about the intended answer first.
