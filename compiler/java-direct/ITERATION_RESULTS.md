# Java-Direct: Iteration Results Log

## Document Purpose

This file captures key findings, decisions, and learnings from each iteration. It serves as:
1. **Progress tracker**: What's been completed
2. **Knowledge base**: Discoveries about the codebase and architecture
3. **Context updater**: Input for updating AGENT_INSTRUCTIONS.md after multiple iterations

**Usage**: After completing each iteration, the agent MUST append a results section below.

**Last Updated**: 2026-02-23

---

## Iteration 2: Local Type Resolution Implementation - 2026-02-23

### Status
- ✅ Completed

### Summary
Implemented local scope resolution for Java supertypes within the same file. Created `LocalJavaScope` class that maps simple class names to `JavaClass` instances, enabling `JC2 extends JC` style inheritance to resolve correctly when both classes are in the same file. Unit tests confirm resolution works, but box tests still fail on constructor resolution issues.

### Key Findings
- **Supertype Resolution Architecture**: `JavaClassifierTypeOverAst.classifier` needs access to scope during lazy evaluation
- **Scope Propagation**: `LocalJavaScope` must be threaded through `JavaClassOverAst` construction and passed to inner classes
- **Test Pattern**: Same error persists (`UNRESOLVED_REFERENCE: '<init>'`), indicating constructor issues are the next blocker
- **Box Test Status**: Still 0% pass rate, all 128 failing tests show constructor resolution errors
- **Local Resolution Works**: Unit test confirms `class Derived extends Base` resolves Base correctly when both in same file

### Implementation Decisions
- **Lazy Resolution**: Used `by lazy` in `JavaClassifierTypeOverAst.classifier` to avoid eager evaluation during construction
- **Scope Creation**: `LocalJavaScope` created once per file at parse time, shared by all classes in that file
- **Simple Names Only**: Current implementation only resolves unqualified names (e.g., `Base`), not qualified names (e.g., `pkg.Base`)
- **Deferred**: FIR integration for external classes, import handling, package-qualified resolution - these are for future iterations

### Changes Made
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/LocalJavaScope.kt`: New file implementing local class name to JavaClass mapping
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt`: Added `localScope` parameter, pass to supertypes and inner classes
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt`: Modified `JavaClassifierTypeOverAst` to accept `localScope` and implement lazy `classifier` resolution
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt`: Create `LocalJavaScope` in `parseTopLevelClassFromFile`
- `compiler/java-direct/test/org/jetbrains/kotlin/java/direct/JavaParsingTest.kt`: Added `testLocalInheritance` unit test

### Test Results
- Unit tests: 9 passing (was 8), 1 added (`testLocalInheritance`)
- Box tests: 0% pass rate (unchanged), 128/128 failing
- Test progression: Same error as Iteration 1, but local resolution logic is verified working
- Unit test confirms: `Base` class resolves correctly in `class Derived extends Base` scenario

### Issues Encountered
- **Same Box Test Errors**: Constructor resolution (`<init>` reference) still failing, blocking all box tests
- **Scope Threading**: Required careful propagation through `JavaClassOverAst` constructors and inner class creation
- **Test Data Structure**: Box tests have multiple files per test, ensuring scope is file-local was important

### Next Layer Issues Identified
Main blocker is now clearly constructor resolution:
1. **Constructor Visibility**: `UNRESOLVED_REFERENCE: '<init>'` suggests Java constructors not properly exposed to Kotlin
2. **Default Constructors**: `JavaClassOverAst.hasDefaultConstructor()` currently returns `false` - may need proper implementation
3. **Constructor Synthetic Members**: Java classes without explicit constructors need synthetic default constructor
4. **Member Resolution**: Beyond constructors, general member access may have issues

### Recommendations for Future Iterations
- **Iteration 3 Priority**: Investigate constructor resolution issue - why is `<init>` unresolved?
- **hasDefaultConstructor**: Implement proper logic to return `true` when no explicit constructors exist
- **Synthetic Constructors**: May need to generate default constructor when `constructors` collection is empty
- **FIR Integration**: Still deferred - need to solve local issues first before tackling external type resolution

### Documentation Updates Needed
- [x] Update ITERATION_RESULTS.md: This entry
- [ ] Update AGENT_INSTRUCTIONS.md: Add LocalJavaScope to "What Works", update "What's Failing" to focus on constructors
- [ ] Update IMPLEMENTATION_PLAN.md: Mark local scope as implemented (Phase 1 complete)

---

## How to Update This File

After completing each iteration, add a new section using this template:

```markdown
## Iteration N: [Title] - [Date]

### Status
- ✅ Completed / ⚠️ Partially completed / ❌ Blocked

### Summary (2-3 sentences)
[What was accomplished]

### Key Findings
- [Discovery about codebase architecture]
- [Unexpected behavior or pattern found]
- [Important file/class/function discovered]

### Implementation Decisions
- [Why approach X was chosen over Y]
- [Trade-offs made]
- [Deferred items]

### Code Quality Check
- [ ] Removed all obvious/redundant comments
- [ ] Kept only comments explaining "why", not "what"
- [ ] Code is self-explanatory where possible

### Changes Made
- `path/to/file.kt`: [Brief description]
- `path/to/test.kt`: [Brief description]

### Test Results
- Unit tests: X passing, Y added
- Box tests: X% pass rate (was Y% before)
- Notable test fixes: [test names]

### Issues Encountered
- [Problem and how it was solved]
- [Blockers and workarounds]

### Recommendations for Future Iterations
- [Adjustments to approach]
- [Things to watch out for]
- [Dependencies discovered]

### Documentation Updates Needed
- [ ] Update AGENT_INSTRUCTIONS.md: [what needs updating]
- [ ] Update IMPLEMENTATION_PLAN.md: [what needs updating]
- [ ] Update FIXING_ITERATIONS.md: [what needs updating]
```

---

## Iteration 2: Local Type Resolution Implementation - 2026-02-23

### Status
- ✅ Completed

### Summary
Implemented local scope resolution for Java supertypes within the same file. Created `LocalJavaScope` class that maps simple class names to `JavaClass` instances, enabling `JC2 extends JC` style inheritance to resolve correctly when both classes are in the same file. Unit tests confirm resolution works, but box tests still fail on constructor resolution issues.

### Key Findings
- **Supertype Resolution Architecture**: `JavaClassifierTypeOverAst.classifier` needs access to scope during lazy evaluation
- **Scope Propagation**: `LocalJavaScope` must be threaded through `JavaClassOverAst` construction and passed to inner classes
- **Test Pattern**: Same error persists (`UNRESOLVED_REFERENCE: '<init>'`), indicating constructor issues are the next blocker
- **Box Test Status**: Still 0% pass rate, all 128 failing tests show constructor resolution errors
- **Local Resolution Works**: Unit test confirms `class Derived extends Base` resolves Base correctly when both in same file

### Implementation Decisions
- **Lazy Resolution**: Used `by lazy` in `JavaClassifierTypeOverAst.classifier` to avoid eager evaluation during construction
- **Scope Creation**: `LocalJavaScope` created once per file at parse time, shared by all classes in that file
- **Simple Names Only**: Current implementation only resolves unqualified names (e.g., `Base`), not qualified names (e.g., `pkg.Base`)
- **Deferred**: FIR integration for external classes, import handling, package-qualified resolution - these are for future iterations

### Changes Made
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/LocalJavaScope.kt`: New file implementing local class name to JavaClass mapping
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt`: Added `localScope` parameter, pass to supertypes and inner classes
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt`: Modified `JavaClassifierTypeOverAst` to accept `localScope` and implement lazy `classifier` resolution
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt`: Create `LocalJavaScope` in `parseTopLevelClassFromFile`
- `compiler/java-direct/test/org/jetbrains/kotlin/java/direct/JavaParsingTest.kt`: Added `testLocalInheritance` unit test

### Test Results
- Unit tests: 9 passing (was 8), 1 added (`testLocalInheritance`)
- Box tests: 0% pass rate (unchanged), 128/128 failing
- Test progression: Same error as Iteration 1, but local resolution logic is verified working
- Unit test confirms: `Base` class resolves correctly in `class Derived extends Base` scenario

### Issues Encountered
- **Same Box Test Errors**: Constructor resolution (`<init>` reference) still failing, blocking all box tests
- **Scope Threading**: Required careful propagation through `JavaClassOverAst` constructors and inner class creation
- **Test Data Structure**: Box tests have multiple files per test, ensuring scope is file-local was important

### Next Layer Issues Identified
Main blocker is now clearly constructor resolution:
1. **Constructor Visibility**: `UNRESOLVED_REFERENCE: '<init>'` suggests Java constructors not properly exposed to Kotlin
2. **Default Constructors**: `JavaClassOverAst.hasDefaultConstructor()` currently returns `false` - may need proper implementation
3. **Constructor Synthetic Members**: Java classes without explicit constructors need synthetic default constructor
4. **Member Resolution**: Beyond constructors, general member access may have issues

### Recommendations for Future Iterations
- **Iteration 3 Priority**: Investigate constructor resolution issue - why is `<init>` unresolved?
- **hasDefaultConstructor**: Implement proper logic to return `true` when no explicit constructors exist
- **Synthetic Constructors**: May need to generate default constructor when `constructors` collection is empty
- **FIR Integration**: Still deferred - need to solve local issues first before tackling external type resolution

### Documentation Updates Needed
- [x] Update ITERATION_RESULTS.md: This entry
- [ ] Update AGENT_INSTRUCTIONS.md: Add LocalJavaScope to "What Works", update "What's Failing" to focus on constructors
- [ ] Update IMPLEMENTATION_PLAN.md: Mark local scope as implemented (Phase 1 complete)

---

## Periodic Context Refresh Process

Every **2-3 iterations**, a human should:

1. **Review this file**: Read all recent iteration results
2. **Update AGENT_INSTRUCTIONS.md**: 
   - Add newly discovered key files
   - Update "What Works" / "What's Failing" sections
   - Add new common pitfalls
   - Update success metrics if expectations changed
3. **Update FIXING_ITERATIONS.md** (if needed):
   - Adjust future iteration prompts based on learnings
   - Add warnings about discovered issues
   - Update example code if patterns changed
4. **Archive old results**: After updates are incorporated, move detailed logs to an archive section at bottom

This keeps the core instruction files lean while preserving institutional knowledge.

---

## Iteration 2: Local Type Resolution Implementation - 2026-02-23

### Status
- ✅ Completed

### Summary
Implemented local scope resolution for Java supertypes within the same file. Created `LocalJavaScope` class that maps simple class names to `JavaClass` instances, enabling `JC2 extends JC` style inheritance to resolve correctly when both classes are in the same file. Unit tests confirm resolution works, but box tests still fail on constructor resolution issues.

### Key Findings
- **Supertype Resolution Architecture**: `JavaClassifierTypeOverAst.classifier` needs access to scope during lazy evaluation
- **Scope Propagation**: `LocalJavaScope` must be threaded through `JavaClassOverAst` construction and passed to inner classes
- **Test Pattern**: Same error persists (`UNRESOLVED_REFERENCE: '<init>'`), indicating constructor issues are the next blocker
- **Box Test Status**: Still 0% pass rate, all 128 failing tests show constructor resolution errors
- **Local Resolution Works**: Unit test confirms `class Derived extends Base` resolves Base correctly when both in same file

### Implementation Decisions
- **Lazy Resolution**: Used `by lazy` in `JavaClassifierTypeOverAst.classifier` to avoid eager evaluation during construction
- **Scope Creation**: `LocalJavaScope` created once per file at parse time, shared by all classes in that file
- **Simple Names Only**: Current implementation only resolves unqualified names (e.g., `Base`), not qualified names (e.g., `pkg.Base`)
- **Deferred**: FIR integration for external classes, import handling, package-qualified resolution - these are for future iterations

### Changes Made
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/LocalJavaScope.kt`: New file implementing local class name to JavaClass mapping
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt`: Added `localScope` parameter, pass to supertypes and inner classes
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt`: Modified `JavaClassifierTypeOverAst` to accept `localScope` and implement lazy `classifier` resolution
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt`: Create `LocalJavaScope` in `parseTopLevelClassFromFile`
- `compiler/java-direct/test/org/jetbrains/kotlin/java/direct/JavaParsingTest.kt`: Added `testLocalInheritance` unit test

### Test Results
- Unit tests: 9 passing (was 8), 1 added (`testLocalInheritance`)
- Box tests: 0% pass rate (unchanged), 128/128 failing
- Test progression: Same error as Iteration 1, but local resolution logic is verified working
- Unit test confirms: `Base` class resolves correctly in `class Derived extends Base` scenario

### Issues Encountered
- **Same Box Test Errors**: Constructor resolution (`<init>` reference) still failing, blocking all box tests
- **Scope Threading**: Required careful propagation through `JavaClassOverAst` constructors and inner class creation
- **Test Data Structure**: Box tests have multiple files per test, ensuring scope is file-local was important

### Next Layer Issues Identified
Main blocker is now clearly constructor resolution:
1. **Constructor Visibility**: `UNRESOLVED_REFERENCE: '<init>'` suggests Java constructors not properly exposed to Kotlin
2. **Default Constructors**: `JavaClassOverAst.hasDefaultConstructor()` currently returns `false` - may need proper implementation
3. **Constructor Synthetic Members**: Java classes without explicit constructors need synthetic default constructor
4. **Member Resolution**: Beyond constructors, general member access may have issues

### Recommendations for Future Iterations
- **Iteration 3 Priority**: Investigate constructor resolution issue - why is `<init>` unresolved?
- **hasDefaultConstructor**: Implement proper logic to return `true` when no explicit constructors exist
- **Synthetic Constructors**: May need to generate default constructor when `constructors` collection is empty
- **FIR Integration**: Still deferred - need to solve local issues first before tackling external type resolution

### Documentation Updates Needed
- [x] Update ITERATION_RESULTS.md: This entry
- [ ] Update AGENT_INSTRUCTIONS.md: Add LocalJavaScope to "What Works", update "What's Failing" to focus on constructors
- [ ] Update IMPLEMENTATION_PLAN.md: Mark local scope as implemented (Phase 1 complete)

---

## Iteration Results

<!-- Agents: Add your results below, newest first -->

### Example Format (Delete this after first real iteration)

## Iteration 0: Example - 2026-02-23

### Status
- ✅ Completed

### Summary
This is an example of how to format iteration results. Real results should follow this structure.

### Key Findings
- Discovered that `FirSession` is available via `JavaClassFinder.Request.session`
- `JavaTypeResolver` should be a top-level class, not nested

### Implementation Decisions
- Chose to implement local scope first before FIR integration
- Used lazy delegation throughout to avoid circular dependencies

### Changes Made
- `compiler/java-direct/src/.../LocalJavaScope.kt`: Created new file for local type resolution
- `compiler/java-direct/test/.../TypeResolutionTest.kt`: Added unit tests

### Test Results
- Unit tests: 5 passing, 5 added
- Box tests: 15% pass rate (was 0% before)
- Notable test fixes: testSimpleInheritance, testLocalClasses

### Issues Encountered
- Initially tried eager resolution, caused stack overflow
- Switched to lazy properties, problem solved

### Recommendations for Future Iterations
- Pay attention to lazy evaluation in all future work
- FIR integration will need careful handling of null returns

### Documentation Updates Needed
- [x] Update AGENT_INSTRUCTIONS.md: Add LocalJavaScope to key files list
- [ ] Update IMPLEMENTATION_PLAN.md: Mark local scope as implemented
- [ ] Update FIXING_ITERATIONS.md: Warn about lazy evaluation in Iteration 3

---

## Iteration 2: Local Type Resolution Implementation - 2026-02-23

### Status
- ✅ Completed

### Summary
Implemented local scope resolution for Java supertypes within the same file. Created `LocalJavaScope` class that maps simple class names to `JavaClass` instances, enabling `JC2 extends JC` style inheritance to resolve correctly when both classes are in the same file. Unit tests confirm resolution works, but box tests still fail on constructor resolution issues.

### Key Findings
- **Supertype Resolution Architecture**: `JavaClassifierTypeOverAst.classifier` needs access to scope during lazy evaluation
- **Scope Propagation**: `LocalJavaScope` must be threaded through `JavaClassOverAst` construction and passed to inner classes
- **Test Pattern**: Same error persists (`UNRESOLVED_REFERENCE: '<init>'`), indicating constructor issues are the next blocker
- **Box Test Status**: Still 0% pass rate, all 128 failing tests show constructor resolution errors
- **Local Resolution Works**: Unit test confirms `class Derived extends Base` resolves Base correctly when both in same file

### Implementation Decisions
- **Lazy Resolution**: Used `by lazy` in `JavaClassifierTypeOverAst.classifier` to avoid eager evaluation during construction
- **Scope Creation**: `LocalJavaScope` created once per file at parse time, shared by all classes in that file
- **Simple Names Only**: Current implementation only resolves unqualified names (e.g., `Base`), not qualified names (e.g., `pkg.Base`)
- **Deferred**: FIR integration for external classes, import handling, package-qualified resolution - these are for future iterations

### Changes Made
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/LocalJavaScope.kt`: New file implementing local class name to JavaClass mapping
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt`: Added `localScope` parameter, pass to supertypes and inner classes
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt`: Modified `JavaClassifierTypeOverAst` to accept `localScope` and implement lazy `classifier` resolution
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt`: Create `LocalJavaScope` in `parseTopLevelClassFromFile`
- `compiler/java-direct/test/org/jetbrains/kotlin/java/direct/JavaParsingTest.kt`: Added `testLocalInheritance` unit test

### Test Results
- Unit tests: 9 passing (was 8), 1 added (`testLocalInheritance`)
- Box tests: 0% pass rate (unchanged), 128/128 failing
- Test progression: Same error as Iteration 1, but local resolution logic is verified working
- Unit test confirms: `Base` class resolves correctly in `class Derived extends Base` scenario

### Issues Encountered
- **Same Box Test Errors**: Constructor resolution (`<init>` reference) still failing, blocking all box tests
- **Scope Threading**: Required careful propagation through `JavaClassOverAst` constructors and inner class creation
- **Test Data Structure**: Box tests have multiple files per test, ensuring scope is file-local was important

### Next Layer Issues Identified
Main blocker is now clearly constructor resolution:
1. **Constructor Visibility**: `UNRESOLVED_REFERENCE: '<init>'` suggests Java constructors not properly exposed to Kotlin
2. **Default Constructors**: `JavaClassOverAst.hasDefaultConstructor()` currently returns `false` - may need proper implementation
3. **Constructor Synthetic Members**: Java classes without explicit constructors need synthetic default constructor
4. **Member Resolution**: Beyond constructors, general member access may have issues

### Recommendations for Future Iterations
- **Iteration 3 Priority**: Investigate constructor resolution issue - why is `<init>` unresolved?
- **hasDefaultConstructor**: Implement proper logic to return `true` when no explicit constructors exist
- **Synthetic Constructors**: May need to generate default constructor when `constructors` collection is empty
- **FIR Integration**: Still deferred - need to solve local issues first before tackling external type resolution

### Documentation Updates Needed
- [x] Update ITERATION_RESULTS.md: This entry
- [ ] Update AGENT_INSTRUCTIONS.md: Add LocalJavaScope to "What Works", update "What's Failing" to focus on constructors
- [ ] Update IMPLEMENTATION_PLAN.md: Mark local scope as implemented (Phase 1 complete)

---

<!-- Real iteration results start here -->

## Iteration 1: Root Cause Analysis & knownClassNamesInPackage Fix - 2026-02-23

### Status
- ✅ Completed

### Summary
Identified and fixed critical bug in `JavaClassFinderOverAstImpl.knownClassNamesInPackage()` that was blocking ALL tests. The method returned `null` for packages not in our index (like `kotlin`, `java.lang`), but FIR expects an empty `Set<String>` to indicate "no Java classes in this package from this source". Changed to return `emptySet()` instead of `null`, which allows tests to progress past the initial failure.

### Key Findings
- **Critical Bug**: `knownClassNamesInPackage()` returning `null` caused `IllegalArgumentException` in `FirCachingCompositeSymbolProvider.computeTopLevelClassifierNames()` at line 48
- **FIR Convention**: `null` means "cannot compute", empty set means "checked, no classes here"
- **Error Chain**: Failure occurred during supertype resolution → type enhancement → type expansion → classifier lookup → package name query
- **Test Pattern**: ALL tests failed with identical error before fix, now progress to different errors (constructor resolution issues)
- **Package Context**: Failure specifically on `kotlin` package query during type mapping checks (e.g., `Object` → `Any`)

### Implementation Decisions
- **Chose**: Return `emptySet()` for packages not in index
- **Rationale**: Matches FIR convention and PSI-based implementation behavior
- **Trade-off**: None - this is strictly more correct than `null`
- **Deferred**: Type resolution, import handling (will be needed for next layer of failures)

### Changes Made
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt:99-107`: Modified `knownClassNamesInPackage()` to return empty set instead of null, added explanatory comments
- `compiler/java-direct/test/org/jetbrains/kotlin/java/direct/JavaParsingTest.kt`: Added `testKnownClassNamesInPackage()` unit test verifying correct behavior for packages in/not in index

### Test Results
- Unit tests: 8 passing (was 7), 1 added (`testKnownClassNamesInPackage`)
- Box tests: 0% pass rate still, but ERROR CHANGED (huge progress!)
  - **Before**: `IllegalArgumentException: classifier names in package kotlin is expected to be not null in CLI`
  - **After**: `UNRESOLVED_REFERENCE: Unresolved reference '<init>'` and diagnostic mismatches
- Tests now compile through FIR supertype resolution phase successfully
- Notable change: Tests reach code generation/execution phase now

### Issues Encountered
- **Initial misdiagnosis**: Considered supertype resolution as root cause, but simpler null-handling bug was blocking everything
- **FIR documentation**: Convention for `null` vs empty set not well documented, had to trace through FIR code
- **Test execution**: Box tests take 10-15 seconds each, slow iteration cycle

### Next Layer Issues Identified
From new test failures, need to address:
1. **Constructor resolution**: `UNRESOLVED_REFERENCE: '<init>'` suggests Java constructors not being exposed correctly
2. **Supertype resolution**: Tests now reach this point, will need implementation (Iteration 2)
3. **Member access**: Methods/fields from Java classes may not be accessible yet
4. **Diagnostic output**: Test expects certain FIR diagnostics that may differ with AST-based implementation

### Recommendations for Future Iterations
- **Iteration 2 Priority**: Focus on supertype resolution (local scope first as planned)
- **Constructor handling**: May need to verify `JavaClassOverAst.constructors` returns correct synthetic constructors
- **Test strategy**: Continue with simple tests (`abstractMethodsOfAny`) to isolate issues
- **Performance**: Consider running smaller subset of tests for faster iteration

### Documentation Updates Needed
- [x] Update ITERATION_RESULTS.md: This entry
- [ ] Update AGENT_INSTRUCTIONS.md: Add to "What Works" - knownClassNamesInPackage fixed
- [ ] Update AGENT_INSTRUCTIONS.md: Update "What's Failing" - tests now fail at different stage

---

## Iteration 2: Local Type Resolution Implementation - 2026-02-23

### Status
- ✅ Completed

### Summary
Implemented local scope resolution for Java supertypes within the same file. Created `LocalJavaScope` class that maps simple class names to `JavaClass` instances, enabling `JC2 extends JC` style inheritance to resolve correctly when both classes are in the same file. Unit tests confirm resolution works, but box tests still fail on constructor resolution issues.

### Key Findings
- **Supertype Resolution Architecture**: `JavaClassifierTypeOverAst.classifier` needs access to scope during lazy evaluation
- **Scope Propagation**: `LocalJavaScope` must be threaded through `JavaClassOverAst` construction and passed to inner classes
- **Test Pattern**: Same error persists (`UNRESOLVED_REFERENCE: '<init>'`), indicating constructor issues are the next blocker
- **Box Test Status**: Still 0% pass rate, all 128 failing tests show constructor resolution errors
- **Local Resolution Works**: Unit test confirms `class Derived extends Base` resolves Base correctly when both in same file

### Implementation Decisions
- **Lazy Resolution**: Used `by lazy` in `JavaClassifierTypeOverAst.classifier` to avoid eager evaluation during construction
- **Scope Creation**: `LocalJavaScope` created once per file at parse time, shared by all classes in that file
- **Simple Names Only**: Current implementation only resolves unqualified names (e.g., `Base`), not qualified names (e.g., `pkg.Base`)
- **Deferred**: FIR integration for external classes, import handling, package-qualified resolution - these are for future iterations

### Changes Made
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/LocalJavaScope.kt`: New file implementing local class name to JavaClass mapping
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt`: Added `localScope` parameter, pass to supertypes and inner classes
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt`: Modified `JavaClassifierTypeOverAst` to accept `localScope` and implement lazy `classifier` resolution
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt`: Create `LocalJavaScope` in `parseTopLevelClassFromFile`
- `compiler/java-direct/test/org/jetbrains/kotlin/java/direct/JavaParsingTest.kt`: Added `testLocalInheritance` unit test

### Test Results
- Unit tests: 9 passing (was 8), 1 added (`testLocalInheritance`)
- Box tests: 0% pass rate (unchanged), 128/128 failing
- Test progression: Same error as Iteration 1, but local resolution logic is verified working
- Unit test confirms: `Base` class resolves correctly in `class Derived extends Base` scenario

### Issues Encountered
- **Same Box Test Errors**: Constructor resolution (`<init>` reference) still failing, blocking all box tests
- **Scope Threading**: Required careful propagation through `JavaClassOverAst` constructors and inner class creation
- **Test Data Structure**: Box tests have multiple files per test, ensuring scope is file-local was important

### Next Layer Issues Identified
Main blocker is now clearly constructor resolution:
1. **Constructor Visibility**: `UNRESOLVED_REFERENCE: '<init>'` suggests Java constructors not properly exposed to Kotlin
2. **Default Constructors**: `JavaClassOverAst.hasDefaultConstructor()` currently returns `false` - may need proper implementation
3. **Constructor Synthetic Members**: Java classes without explicit constructors need synthetic default constructor
4. **Member Resolution**: Beyond constructors, general member access may have issues

### Recommendations for Future Iterations
- **Iteration 3 Priority**: Investigate constructor resolution issue - why is `<init>` unresolved?
- **hasDefaultConstructor**: Implement proper logic to return `true` when no explicit constructors exist
- **Synthetic Constructors**: May need to generate default constructor when `constructors` collection is empty
- **FIR Integration**: Still deferred - need to solve local issues first before tackling external type resolution

### Documentation Updates Needed
- [x] Update ITERATION_RESULTS.md: This entry
- [ ] Update AGENT_INSTRUCTIONS.md: Add LocalJavaScope to "What Works", update "What's Failing" to focus on constructors
- [ ] Update IMPLEMENTATION_PLAN.md: Mark local scope as implemented (Phase 1 complete)

---


