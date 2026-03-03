# Java-Direct: Iteration Results Log

## Document Purpose

This file captures key findings, decisions, and learnings from each iteration. It serves as:
1. **Progress tracker**: What's been completed
2. **Knowledge base**: Discoveries about the codebase and architecture
3. **Context updater**: Input for updating AGENT_INSTRUCTIONS.md after multiple iterations

**Usage**: After completing each iteration, the agent MUST append a results section below.

**Last Updated**: 2026-03-03

---

## Iterations 1-6: Completed (Archived)

**Status**: ✅ All completed  
**Final Result**: 90/138 (65.2%) box tests passing  
**Archive**: See `implDocs/archive/ITERATIONS_1_6_DETAILS.md` for full details

### Progress Summary

| Iteration | Date | Focus | Tests Before | Tests After | Key Change |
|-----------|------|-------|--------------|-------------|------------|
| 1 | 2026-02-23 | Constructor Analysis | 0/138 | 1/138 | Fixed `hasDefaultConstructor()` |
| 2 | 2026-02-24 | Type Resolution Architecture | 1/138 | 1/138 | Verified classifierQualifiedName approach |
| 3 | 2026-02-24 | Import Handling | 1/138 | 11/138 | Implemented JavaImports |
| 4 | 2026-02-25 | Star Imports + Parameters | 11/138 | 30/138 | Callback resolution + parameter parsing |
| 5 | 2026-02-27 | Type Arguments | 30/138 | 31/138 | Generic type arguments + visibility fix |
| 6 | 2026-03-03 | Hybrid JavaClassFinder | 31/138 | 90/138 | Combined source+binary class finding |

### Key Architectural Decisions

1. **Type Resolution in FIR Layer** (Iteration 2): Java Model provides names, FIR provides resolution. No `FirSession` access in Java Model.

2. **Callback Pattern for Star Imports** (Iteration 4): `resolve(tryResolve: (String) -> Boolean)` allows Java Model to implement Java resolution rules while FIR validates existence.

3. **Hybrid Finder Architecture** (Iteration 6): Source-first, binary-fallback via `CombinedJavaClassFinder`. Added `defaultFinderProvider` parameter to `JavaClassFinderFactory`.

### Key Files Modified (Iterations 1-6)

- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt` - Java class model
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt` - Type representations
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt` - Methods, fields, parameters
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaImports.kt` - Import handling (NEW)
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaDirectComponentRegistrar.kt` - Factory with hybrid finder
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt` - Source class finder
- `compiler/cli/src/org/jetbrains/kotlin/cli/jvm/compiler/extensions/JavaClassFinderFactory.kt` - Added defaultFinderProvider
- `compiler/cli/src/org/jetbrains/kotlin/cli/jvm/compiler/VfsBasedProjectEnvironment.kt` - Passes defaultFinderProvider
- `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaTypes.kt` - Added isResolved/resolve()
- `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt` - Uses resolve callback

### Remaining Failures (48 tests)

After Iteration 6, 48 tests still fail. Likely causes:
1. **Type parameter handling**: `T`, `U`, `S` being treated as class names
2. **Generics/wildcards**: Complex generic signatures (`? extends`, `? super`)
3. **SAM lambda inference**: `it` parameter not resolved
4. **Other semantic issues**: Not related to class finding

---

## Template for Future Iterations

```markdown
## Iteration N: [Title] - YYYY-MM-DD

### Status
- [ ] In Progress / ✅ Completed

### Summary
[2-3 sentences describing what was done and the result]

### Key Findings
[Bullet points of important discoveries]

### Changes Made
[List of files and what changed]

### Test Results
- Unit tests: X/Y passing
- Box tests: X/138 passing (X%) - UP/DOWN from X/138 (X%)

### Issues Encountered
[Problems hit and how they were solved]

### Key Learnings
[What should be remembered for future work]
```

---

## Future Iterations Start Below

<!-- Add new iteration results here, newest at top -->
