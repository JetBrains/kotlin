---
name: analysis-extend-ka-resolver
description: Add KaResolver resolveSymbol/resolveCall support for a PSI type
user-invocable: true
disable-model-invocation: true
argument-hint: <KtPsiType>
---

# Add KaResolver support for a PSI type

This skill adds `resolveSymbol()` and optionally `resolveCall()` support for a given `Kt*` PSI type
by following the established pattern from existing resolver support commits.

The argument is the PSI type name, e.g. `KtDestructuringDeclarationEntry`.

---

## Phase 1: Gather information

1. **Find the PSI type source file.** Search `compiler/psi/` for `<KtPsiType>.java` or `<KtPsiType>.kt`.
   Read the file to understand the class hierarchy and whether it already implements `KtResolvable` or `KtResolvableCall`.

2. **Check KaResolver for existing support.** Read `analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api/components/KaResolver.kt`
   and search for the PSI type. If it already has `resolveSymbol()`/`resolveCall()` methods, inform the user and stop.

3. **Check for existing test data.** Search `analysis/analysis-api/testData/components/resolver/` for test files
   mentioning the PSI type or related scenarios.

4. **Read the Analysis API AGENTS.md** at `analysis/AGENTS.md` for area-specific guidelines.

---

## Phase 2: Ask user questions

Use `AskUserQuestion` to ask these questions (all in one call):

### Question 1: Resolution kind
**Header:** "Resolution"
**Question:** "Should `<KtPsiType>` support symbol-only resolution (`KtResolvable`) or both symbol and call resolution (`KtResolvableCall`)?"
- `KtResolvable` — symbol resolution only (`resolveSymbol()`)
- `KtResolvableCall` — both symbol and call resolution (`resolveSymbol()` + `resolveCall()`)

### Question 2: Symbol return type
**Header:** "Symbol type"
**Question:** "What should `resolveSymbol()` return for `<KtPsiType>`?"
- `KaConstructorSymbol`
- `KaFunctionSymbol`
- `KaNamedFunctionSymbol`
- `KaCallableSymbol`

(Allow "Other" for types like `KaDeclarationSymbol`, etc.)

### Question 3: Call return type (only if `KtResolvableCall`)
**Header:** "Call type"
**Question:** "What should `resolveCall()` return for `<KtPsiType>`?"
- `KaFunctionCall<KaConstructorSymbol>`
- `KaDelegatedConstructorCall`
- `KaAnnotationCall`
- `KaFunctionCall<KaNamedFunctionSymbol>`

(Allow "Other" for types like `KaSingleCall<*, *>`, `KaFunctionCall<*>`, etc.)

---

## Phase 3: Execute changes

Use the answers from Phase 2 to determine: `RESOLUTION_KIND` (`KtResolvable` or `KtResolvableCall`),
`SYMBOL_TYPE` (e.g. `KaCallableSymbol`), and `CALL_TYPE` (e.g. `KaSingleCall<*, *>`).

### Step 1: PSI type — add interface implementation

**File:** The PSI source file found in Phase 1 (under `compiler/psi/`).

- If the PSI type does NOT already implement `KtResolvable`/`KtResolvableCall`, add it:
  - For `KtResolvable`: add `implements KtResolvable` (Java) or `: KtResolvable` (Kotlin)
  - For `KtResolvableCall`: add `implements KtResolvableCall` (Java) or `: KtResolvableCall` (Kotlin)
  - `KtResolvableCall` extends `KtResolvable`, so only one is needed.
- Add the necessary import (`org.jetbrains.kotlin.resolution.KtResolvable` or `org.jetbrains.kotlin.resolution.KtResolvableCall`).

### Step 2: KaResolver interface — add methods

**File:** `analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api/components/KaResolver.kt`

#### 2a: Add `resolveSymbol()` interface method

Insert **after** the last existing typed `resolveSymbol()` method (currently `KtDestructuringDeclarationEntry.resolveSymbol()`)
and **before** `tryResolveCall()`.

Follow the exact KDoc pattern — copy from a similar existing method and adapt:

```kotlin
    /**
     * Resolves the <description> by the given [<KtPsiType>].
     *
     * #### Example
     *
     * ```kotlin
     * <code example with // ^^^^ markers>
     * ```
     *
     * Calling `resolveSymbol()` on a [<KtPsiType>] ... returns the [<SYMBOL_TYPE>] ...
     * if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvable.resolveSymbol] focused specifically on <description>
     *
     * @see tryResolveSymbols
     * @see KtResolvable.resolveSymbol
     */
    @KaExperimentalApi
    public fun <KtPsiType>.resolveSymbol(): <SYMBOL_TYPE>?
```

#### 2b: Add `resolveCall()` interface method (only if `KtResolvableCall`)

Insert **after** the last existing typed `resolveCall()` method (currently `KtDestructuringDeclarationEntry.resolveCall()`)
and **before** `collectCallCandidates()`.

```kotlin
    /**
     * Resolves the given [<KtPsiType>] to a <call description>.
     *
     * #### Example
     *
     * ```kotlin
     * <code example with // ^^^^ markers>
     * ```
     *
     * Returns the corresponding [<CALL_TYPE short name>] if resolution succeeds; otherwise, it returns `null`
     * (e.g., when unresolved or ambiguous).
     *
     * This is a specialized counterpart of [KtResolvableCall.resolveCall] focused specifically on <description>
     *
     * @see tryResolveCall
     * @see KtResolvableCall.resolveCall
     */
    @KaExperimentalApi
    public fun <KtPsiType>.resolveCall(): <CALL_TYPE>?
```

#### 2c: Add `resolveSymbol()` bridge function

Insert **after** the last existing `resolveSymbol` bridge (currently `KtDestructuringDeclarationEntry.resolveSymbol` bridge)
and **before** the `tryResolveCall` bridge.

```kotlin
/**
 * <Same KDoc as the interface method>
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun <KtPsiType>.resolveSymbol(): <SYMBOL_TYPE>? {
    return with(session) {
        resolveSymbol()
    }
}
```

#### 2d: Add `resolveCall()` bridge function (only if `KtResolvableCall`)

Insert **after** the last existing `resolveCall` bridge (currently `KtDestructuringDeclarationEntry.resolveCall` bridge)
and **before** the `collectCallCandidates` bridge.

```kotlin
/**
 * <Same KDoc as the interface method>
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun <KtPsiType>.resolveCall(): <CALL_TYPE>? {
    return with(session) {
        resolveCall()
    }
}
```

### Step 3: KaBaseResolver — add override implementations

**File:** `analysis/analysis-api-impl-base/src/org/jetbrains/kotlin/analysis/api/impl/base/components/KaBaseResolver.kt`

#### 3a: Add `resolveSymbol()` override (always)

Insert **after** the last existing `resolveSymbolSafe()` line (currently `KtDestructuringDeclarationEntry.resolveSymbol()`)
and **before** `KtReference.resolveToSymbol()`.

```kotlin
    final override fun <KtPsiType>.resolveSymbol(): <SYMBOL_TYPE>? = resolveSymbolSafe()
```

#### 3b: Add `resolveCall()` override (only if `KtResolvableCall`)

Insert **after** the last existing `resolveCallSafe()`/`resolveSingleCallSafe()` line
(currently `KtDestructuringDeclarationEntry.resolveCall()`) and **before** `KtElement.resolveToCall()`.

Choose the helper based on the call return type:
- If CALL_TYPE contains wildcards (`*`) → use `resolveCallSafe()`:
  ```kotlin
      final override fun <KtPsiType>.resolveCall(): <CALL_TYPE>? = resolveCallSafe()
  ```
- If CALL_TYPE is fully specified (no wildcards) → use `resolveSingleCallSafe()`:
  ```kotlin
      final override fun <KtPsiType>.resolveCall(): <CALL_TYPE>? = resolveSingleCallSafe()
  ```

#### 3c: Add to `canBeResolvedAsCall` (only if `KtResolvableCall`)

In the `canBeResolvedAsCall` function, add a new branch **before** `else -> false`:

```kotlin
        is <KtPsiType> -> true
```

### Step 4: Investigate FIR/FE10 resolver changes

**This step requires investigation — do NOT skip it.**

Read the FIR resolver:
- **File:** `analysis/analysis-api-fir/src/org/jetbrains/kotlin/analysis/api/fir/components/KaFirResolver.kt`

The FIR resolver works by calling `getOrBuildFir(psi)` and dispatching on the FIR element type in a `when` block.
Investigate:
1. What FIR element type does `getOrBuildFir(<KtPsiType instance>)` return?
2. Is that FIR element type already handled in the `when` blocks of `performSymbolResolution()` and `performCallResolution()`?
3. If NOT handled, add appropriate handling (new branch in the `when`, possibly with unwrapping logic).

**Examples of when FIR changes were needed:**
- `KtDestructuringDeclarationEntry` → maps to `FirProperty` (a declaration), needed to unwrap `FirProperty.initializer`
- `KtLabelReferenceExpression` → FIR doesn't have a dedicated label element, needed to extract from `FirThisReceiverExpression.calleeReference`
- `KtConstructorDelegationReferenceExpression` → needed to add `FirReference` as a handled case
- `KtReturnExpression` → `FirReturnExpression` wasn't handled, added a new branch + helper

Similarly, read the FE10 resolver:
- **File:** `analysis/analysis-api-fe10/src/org/jetbrains/kotlin/analysis/api/descriptors/components/KaFe10Resolver.kt`

Check if the `BindingContext`-based resolution handles the PSI type. Examples of needed changes:
- `KtCallableReferenceExpression` → redirects to `psi.callableReference`
- `KtWhenConditionInRange` → redirects to `psi.operationReference`
- `KtReturnExpression` → custom logic to find enclosing function via `parents()`

If changes are needed, implement them following the existing patterns in those files.

### Step 5: Update PSI API dump

Run:
```bash
./gradlew :compiler:psi:psi-api:updateKotlinAbi
```

This updates `compiler/psi/psi-api/api/psi-api.api` to reflect the new interface implementation.

---

## Phase 4: Verify

### Step 1: Static analysis

Run `get_file_problems` with `errorsOnly=false` on each modified file. Fix any warnings related to the changes.

### Step 2: Update golden test data

```bash
./gradlew manageTestDataGlobally --mode=update --golden-only --test-data-path=analysis/analysis-api/testData/components/resolver/
```

Use `--golden-only` to update only baseline `.txt` files (`.call.txt`, `.symbol.txt`, `.candidates.txt`, `.references.txt`), skipping variant-specific files like `.descriptors.*.txt`. This is faster and establishes the baseline first.

### Step 3: Validate generated test data

Read the newly generated/updated golden `.txt` files and sanity-check:
- `.symbol.txt` — should contain `KaSymbolResolutionSuccess` with the expected symbol type matching `SYMBOL_TYPE`
- `.call.txt` — (if `KtResolvableCall`) should contain `KaCallResolutionSuccess` with the expected call type
- If any file shows `null` or unexpected resolution failures, investigate whether FIR/FE10 changes (Step 4 in Phase 3) are missing

For quick investigation of individual tests, run on a specific subdirectory or file:
```bash
# By subdirectory
./gradlew manageTestDataGlobally --mode=update --golden-only --test-data-path=analysis/analysis-api/testData/components/resolver/singleByPsi/<specific-subdir>/

# By individual file
./gradlew manageTestDataGlobally --mode=update --golden-only --test-data-path=analysis/analysis-api/testData/components/resolver/singleByPsi/<subdir>/TestName.kt
```

### Step 4: Update all variants

```bash
./gradlew manageTestDataGlobally --mode=update --test-data-path=analysis/analysis-api/testData/components/resolver/
```

Full update including `.descriptors.*.txt` variant files.

---

## Phase 5: Commit

Create a commit with the message:
```
[Analysis API] resolver: support new API for `<KtPsiType>`

^KT-66039
```

**Before committing**, read `docs/code_authoring_and_core_review.md` for commit guidelines.