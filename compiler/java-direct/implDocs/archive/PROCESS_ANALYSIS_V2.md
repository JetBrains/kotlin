# Java-Direct Development Process Analysis V2

**Date**: 2026-03-13
**Scope**: Analysis of iterations 24-27, building on prior PROCESS_ANALYSIS.md
**Sources**: AGENT_INSTRUCTIONS.md, IMPLEMENTATION_PLAN.md, FIXING_ITERATIONS.md, ITERATION_RESULTS.md, TEST_FAILURE_ANALYSIS.md, chat log (chat-1f818505), JavaClassOverAst.kt, JavaResolutionContext.kt

---

## Executive Summary

The development has reached **95.3% pass rate (2524/2649)** but iteration efficiency has degraded significantly. Iterations 24-27 fixed 25 tests across 6 sub-iterations, averaging ~4 tests per iteration — down from ~50/iteration in early phases and ~8/iteration in the 17-23 phase.

The slowdown has both **objective causes** (remaining problems are genuinely harder, more isolated edge cases) and **process causes** (estimation failures, insufficient pre-debugging, document sprawl, premature conclusions). This analysis focuses on the process causes since those are actionable.

---

## 1. Estimation: Still Broken Despite Multiple Interventions

### The Data

| Iteration | Category | Estimated | Actual Fixed | Accuracy |
|-----------|----------|-----------|--------------|----------|
| 17 | Annotation args | ~30 | 4 | 13% |
| 22 | TYPE_USE filtering | ~15 | 1 | 7% |
| 25 | Inherited inner classes | 30-40 | 2 | **5-7%** |
| 26 | Sealed classes (initial attempt) | 12-15 | 0 | **0%** |
| 26 | Sealed classes (after AST fix) | 12-15 | 9 | 60% |
| 27 | Java records | 6-8 | 0 | **0%** |

Despite adding "CRITICAL" warnings, post-mortems, and "MANDATORY Estimation Process" sections to documents, accuracy hasn't improved.

### Why Interventions Failed

The documents tell the agent *what to do* (debug 2-3 tests first, categorize by code path) but then provide **pre-written code snippets** that undermine the process. When an agent sees:

```kotlin
override val isSealed: Boolean
    get() = hasModifier("SEALED_KEYWORD")
```

...it implements the snippet directly instead of debugging first. The snippet creates false confidence that the solution is known.

**Iteration 26 case study**: The plan provided code using `"SEALED_KEYWORD"`. Agent implemented it verbatim, ran tests, found no improvement, committed the non-working code, declared it done with "tests still fail, indicating a deeper issue (likely parser support or FIR integration)." Only after the user asked to verify parser support did the agent discover the token is actually `"SEALED"` — which could have been found in 5 minutes with exception-based AST dumping.

### Recommendation

**Remove code snippets from FIXING_ITERATIONS.md entirely.** Replace with:
- Problem description (what's wrong)
- Which tests to debug first
- Which reference implementations to check
- Expected approach *category* (e.g., "implement property in JavaClassOverAst" not the actual code)

---

## 2. Agent Behavioral Patterns

### A. Premature Conclusions

Multiple instances where the agent drew wrong conclusions without sufficient verification:

| Instance | Wrong Conclusion | User Correction |
|----------|-----------------|-----------------|
| Iter 25 | `testInheritedInnerAndNested` is "a FIR-level issue, not java-direct specific" | "It passes fine everywhere except java-direct. Check FirLightTreeBlackBoxCodegenTestGenerated" |
| Iter 25 | Baseline diffs are acceptable/cosmetic | "We do not expect baseline diffs to be a good explanation... assume problem is on java-direct side" |
| Iter 26 | "Tests still fail, indicating a deeper issue (likely parser support)" | "Please check the hypothesis that sealed classes are not yet supported by the parser" |

**Pattern**: The agent reaches for the most convenient explanation (not our problem / parser limitation / baseline diff) rather than debugging further. This is likely a context-length optimization — concluding quickly saves tokens — but it wastes user time on corrections.

**Recommendation**: Add to AGENT_INSTRUCTIONS.md: **"When a fix doesn't work as expected, the default assumption is that the implementation is wrong, not that there's an external/systemic blocker. Debug further before concluding the issue is outside java-direct."**

### B. Unauthorized Commits

In iteration 26, the agent committed code without user review. The user responded: *"Please NEVER perform git commit yourself. I need to review the changes first."*

AGENT_INSTRUCTIONS.md has no commit policy. This is a gap.

**Recommendation**: Add explicit rule: **"NEVER create git commits. All changes must be reviewed by the user before committing."**

### C. Excessive Trial-and-Error

Iteration 25 shows 4 consecutive StackOverflowError cycles:
1. Access `classifier` on supertypes → StackOverflow
2. Access `classifierQualifiedName` on supertypes → StackOverflow
3. Use `localClassProvider` → works but only for same-file
4. Add package qualification → fixes cross-file

Each cycle: edit → compile → run → read error → edit again. This consumed significant context.

**Missing step**: Before coding, trace the data flow on paper. Understanding that `classifier` triggers `findLocalClass` triggers `findInnerClassFromSupertypes` triggers `supertypes` creates a cycle could have been determined by reading the code, not by running it 4 times.

**Recommendation**: Add to iteration workflow: **"Before implementing, trace the call chain from the entry point to understand what gets triggered. Identify potential cycles or side effects."**

### D. Test Name Confusion

The chat log shows repeated failures to construct correct test class names:
- `JavaUsingAstPhasedTestGenerated.Javac.Inheritance.testInheritedInner2` — doesn't exist
- `JavaUsingAstPhasedTestGenerated$Javac$Inheritance` — wrong syntax
- Multiple `find` commands to locate the right class

**Recommendation**: Add a **test name cheat sheet** to AGENT_INSTRUCTIONS.md:
```
# Box tests:
./gradlew :kotlin-java-direct:test --tests "JavaUsingAstBoxTestGenerated.*testName*"

# Phased tests:
./gradlew :kotlin-java-direct:test --tests "JavaUsingAstPhasedTestGenerated.*testName*"

# Use wildcard glob to avoid needing exact nested class path
```

### E. Context Loss on Conversation Restore

The chat log shows a conversation restore between messages 10 and 11. After restore:
- Agent had to re-investigate `testInheritedInnerAndNested` from scratch
- Lost the incorrect conclusion it had made (which was actually useful context)
- The summary didn't capture investigation state

**Recommendation**: Add **"Investigation State" section** to ITERATION_RESULTS.md entries:
```markdown
### In-Progress Investigation (if stopping mid-work)
- **Current test**: testFoo
- **Hypothesis**: X is caused by Y
- **Tried**: A (failed because B), C (partially worked)
- **Next step**: Try D
```

---

## 3. Document Quality

### A. Metric Inconsistency

At the time of analysis, different documents report different numbers:

| Document | Reported Status |
|----------|----------------|
| AGENT_INSTRUCTIONS.md header | Iteration 23 complete, ~62 failing |
| FIXING_ITERATIONS.md header | Iteration 25c complete, 127 failed |
| ITERATION_RESULTS.md header | 125 total failures |
| TEST_FAILURE_ANALYSIS.md | 127 failed |
| Actual (post iter 27) | 125 failed |

**Recommendation**: **Single source of truth** — only FIXING_ITERATIONS.md header carries current metrics. Other documents reference it or carry no metrics at all.

### B. Document Sprawl

Root-level markdown files in `compiler/java-direct/`:

| File | Lines | Purpose | Overlap |
|------|-------|---------|---------|
| AGENT_INSTRUCTIONS.md | 373 | Agent guide | Contains architecture, debugging, workflow, remaining work |
| IMPLEMENTATION_PLAN.md | 162 | Architecture | Overlaps AGENT_INSTRUCTIONS architecture section |
| FIXING_ITERATIONS.md | 203 | Iteration plans | Overlaps TEST_FAILURE_ANALYSIS categories |
| ITERATION_RESULTS.md | 457 | Results log | Growing — latest entries not archived |
| TEST_FAILURE_ANALYSIS.md | 290 | Failure categories | Duplicates FIXING_ITERATIONS remaining work |
| PROCESS_ANALYSIS.md | 317 | Process review | Outdated (covers only iter 17-23) |
| housekeeping.md | ~50 | Meta-tasks | Active |

Plus ~13 files in `implDocs/archive/`.

**Problem**: An agent starting an iteration must process ~1800 lines of markdown before writing any code. Key rules get buried.

**Recommendation**: Restructure to 3 core documents:
1. **AGENT_INSTRUCTIONS.md** (~100 lines) — Ground rules, mandatory checklist, commit policy, test commands
2. **ITERATION_PLAN.md** (renamed from FIXING_ITERATIONS) — Current state, next iterations, approach
3. **ITERATION_LOG.md** (renamed from ITERATION_RESULTS) — Append-only log with archiving

Move architecture, debugging guide, and reference implementations to `implDocs/` (consulted on-demand, not read every iteration).

### C. Code Snippets in Plans

FIXING_ITERATIONS.md provides code like:
```kotlin
override val isSealed: Boolean
    get() = hasModifier("SEALED_KEYWORD")  // WRONG — actual token is "SEALED"
```

This led the agent to implement non-working code and skip debugging. See Section 1 for details.

### D. AGENT_INSTRUCTIONS.md Length

At 373 lines, AGENT_INSTRUCTIONS.md covers:
- Ground rules (20 lines)
- Architecture decisions (60 lines)
- Shared vs java-direct files (40 lines)
- Debugging techniques (35 lines)
- Lessons learned (50 lines)
- Iteration workflow (80 lines)
- Key files (30 lines)
- Pitfalls (15 lines)
- Testing (20 lines)
- Remaining work (25 lines)

An agent must absorb all of this before starting. The most critical information (ground rules, shared file warning, mandatory debugging steps) gets diluted.

---

## 4. Code Quality Assessment

### Positive Patterns

- **Callback pattern** consistently applied across all resolution features — good architectural discipline
- **Lazy evaluation** (`by lazy`) used appropriately to avoid circular dependencies
- **Minimal code** — solutions are generally focused and don't over-engineer
- **Reference implementations** (javac-wrapper, PSI) are consulted for correctness

### Issues Found

**A. `JAVA_LANG_TYPES` Hardcoded Map (Iteration 24, JavaTypeOverAst.kt)**

The agent added a hardcoded map of common `java.lang` types (String, Object, Integer, etc.) for resolving unqualified type names. This contradicts the "no hardcoded lists, use callback pattern" principle that AGENT_INSTRUCTIONS.md explicitly calls "CRITICAL."

The callback approach would be: in `resolveSimpleName`, try `"java.lang.$simpleName"` as a candidate via `tryResolve`, similar to how star imports are already handled.

**B. Dead Code in `isSealed` (Iteration 26)**

```kotlin
override val isSealed: Boolean get() = hasModifier("SEALED") || hasModifier("SEALED_KEYWORD")
```

The `hasModifier("SEALED_KEYWORD")` branch is dead code — the parser never produces this token. The agent added it as a safety net because it wasn't confident in the discovery. This kind of belt-and-suspenders fallback clutters the code and suggests the debugging wasn't thorough enough to build confidence.

**C. Fragile String Parsing in Supertype Resolution (Iteration 25, JavaResolutionContext.kt)**

```kotlin
val supertypeRef = supertype.presentableText.let { text ->
    val withoutGenerics = text.substringBefore('<').trim()
    withoutGenerics.substringBefore('.').trim()
}
```

Parsing `presentableText` with `substringBefore` operations is fragile:
- `substringBefore('.')` on `"java.util.List"` returns `"java"` — wrong
- Only works for same-package unqualified supertypes
- Should use the AST node's IDENTIFIER child or a dedicated accessor

**D. Incomplete Records Implementation (Iteration 27)**

`JavaRecordComponentOverAst.kt` exists and `recordComponents` is implemented in `JavaClassOverAst.kt`, but no tests pass because FIR integration is needed. This is partial/dead code that shouldn't have been committed without a clear path to completion.

---

## 5. The AST Node Name Discovery Problem

This is the **single biggest time-waster** across recent iterations. The pattern repeats:

1. Plan says use token name X (e.g., `SEALED_KEYWORD`)
2. Agent implements using X
3. Tests don't improve
4. Agent investigates other hypotheses (parser doesn't support feature, FIR issue, etc.)
5. Eventually adds exception-based AST dump
6. Discovers actual token name is Y (e.g., `SEALED`)
7. Fixes implementation

This happened in:
- Iteration 26: `SEALED_KEYWORD` → `SEALED`
- Iteration 17b: `METHOD` → `ANNOTATION_METHOD` (for annotation interface methods)
- Various earlier iterations: discovering `ERROR_ELEMENT` behavior, `ENUM_CONSTANT` vs `FIELD`, etc.

**Root cause**: There is no AST reference test. Each new feature requires runtime discovery of token names.

**Recommendation**: Create `JavaModernFeaturesAstTest` in `JavaParsingTest.kt` that:
1. Parses a Java file containing sealed classes, records, permits, text blocks, pattern matching, etc.
2. Dumps or asserts all AST node type names
3. Serves as a reference for agents implementing new features

This is a one-time investment that eliminates the recurring discovery cost.

---

## 6. Baseline Diff Handling

~50-60 tests are categorized as "Baseline Diffs" with LOW priority. The user explicitly pushed back: *"We do not expect baseline diffs to be a good explanation."*

Investigation revealed that many "baseline diffs" are **real failures** where java-direct produces different (wrong) diagnostics than PSI-based FIR. The `testInheritedInnerAndNested` case was initially dismissed as "baseline diff" but turned out to be a real bug (missing `isStatic` for interface-nested classes).

**Recommendation**:
1. Stop using "baseline diff" as a low-priority catch-all
2. For each baseline diff test, verify whether PSI-based FIR passes the same test
3. If PSI passes but java-direct doesn't, it's a java-direct bug — categorize by actual symptom
4. Only remaining true baseline diffs (both PSI and java-direct differ from expected) can be LOW priority

---

## 7. Summary of Recommendations

### Priority 1: Immediate (Next Iteration)

| # | Recommendation | Impact |
|---|---------------|--------|
| 1 | **Create AST node reference test** — parse modern Java, assert/dump all token names | Eliminates #1 time-waster |
| 2 | **Remove code snippets** from FIXING_ITERATIONS.md; keep problem descriptions only | Prevents false confidence |
| 3 | **Add commit policy**: "NEVER commit without user approval" | Prevents unauthorized changes |
| 4 | **Add pre-implementation checklist** (verify AST names, check reference impl, debug 2-3 tests) | Reduces trial-and-error |
| 5 | **Default assumption rule**: "When a fix doesn't work, the implementation is wrong, not the system" | Prevents premature conclusions |

### Priority 2: Near-Term (Within 2-3 Iterations)

| # | Recommendation | Impact |
|---|---------------|--------|
| 6 | **Restructure documents**: Slim AGENT_INSTRUCTIONS to ~100 lines, move details to implDocs/ | Reduces context burden |
| 7 | **Single source of truth for metrics**: Only FIXING_ITERATIONS header | Eliminates stale numbers |
| 8 | **Triage baseline diffs**: Verify each against PSI-based FIR | May reveal 10-20+ real bugs |
| 9 | **Add investigation state** section to ITERATION_RESULTS entries | Survives context loss |
| 10 | **Test name cheat sheet** with wildcard patterns | Eliminates name confusion |

### Priority 3: Longer-Term

| # | Recommendation | Impact |
|---|---------------|--------|
| 11 | **Clean up dead code**: `SEALED_KEYWORD` fallback, assess `JAVA_LANG_TYPES` map | Code quality |
| 12 | **Fix fragile string parsing** in `findInnerClassFromSupertypes` | Correctness |
| 13 | **Smaller iteration scope**: 1-2 tests per iteration, verify thoroughly | Better accuracy |
| 14 | **Improve categorization script**: More granular patterns, track accuracy | Better planning |

---

## 8. What's Working Well (Keep Doing)

- **Callback pattern architecture** — sound, consistently applied, enables clean separation
- **Exception-based debugging** — effective when actually used (the problem is not using it early enough)
- **Reference implementation comparison** — javac-wrapper and PSI provide correct patterns
- **User review checkpoints** — stopping after each iteration for review catches issues early
- **Document archiving** — moving old iterations to `implDocs/archive/` keeps context manageable
- **Ad-hoc debugging over detailed upfront plans** — proven more effective for edge cases

---

*Generated: 2026-03-13*
