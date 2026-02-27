# Java-Direct: Agent Instructions

## Document Purpose

This document contains common instructions, guidelines, and reference information for AI agents working on the `java-direct` module. It should be referenced at the start of each iteration.

**Target Audience**: AI coding assistants (Claude 4.5 Sonnet, Junie, etc.)  
**Usage**: Reference this file at the start of each iteration from FIXING_ITERATIONS.md  
**Status**: Active  
**Last Updated**: 2026-02-23

---

## Overview of Current State

### What Works
- ✅ **KMP Java Parser integration**: Can parse Java source files into AST
- ✅ **Basic Java Model**: `JavaClassOverAst`, `JavaMethodOverAst`, `JavaFieldOverAst` etc. exist
- ✅ **Test infrastructure**: Tests are generated and running through new implementation
- ✅ **Plugin registration**: `JavaDirectComponentRegistrar` successfully injects `JavaClassFinderOverAstImpl`
- ✅ **File indexing**: Basic file discovery and indexing works

### What's Failing
- ❌ **Most box tests**: ~50+ generated tests in `JavaUsingAstLegacyBoxTestGenerated.java` are failing
- ❌ **Type resolution**: Java types referenced in source are not being resolved correctly
- ❌ **Supertypes**: Java class inheritance is not working
- ❌ **Members access**: Methods and fields from Java classes may not be accessible from Kotlin
- ❌ **Package resolution**: Java packages may not be found properly

### Repository Structure
```
compiler/java-direct/
├── src/                          # Implementation
│   └── org/jetbrains/kotlin/java/direct/
│       ├── JavaClassFinderOverAstImpl.kt    # Main entry point
│       ├── JavaClassOverAst.kt              # Java class model
│       ├── JavaMemberOverAst.kt             # Methods, fields, constructors
│       ├── JavaTypeOverAst.kt               # Type representations
│       ├── JavaPackageOverAst.kt            # Package model
│       ├── JavaAnnotationOverAst.kt         # Annotations
│       ├── JavaElementOverAst.kt            # Base classes
│       ├── JavaDirectComponentRegistrar.kt  # Plugin registration
│       ├── parse.kt                         # Parser integration
│       └── utils.kt                         # Utilities
├── test/                         # Unit tests
│   └── org/jetbrains/kotlin/java/direct/
│       └── JavaParsingTest.kt              # Parser tests (passing)
├── testFixtures/                 # Test infrastructure
│   └── org/jetbrains/kotlin/java/direct/
│       ├── AbstractJavaUsingAstBoxTest.kt  # Box test base
│       ├── AbstractJavaUsingAstTest.kt     # Diagnostic test base
│       ├── components.kt                   # Test configurator
│       └── TestGenerator.kt                # Test generator
├── build/tests-gen/              # Generated tests
│   └── org/jetbrains/kotlin/java/direct/
│       ├── JavaUsingAstLegacyBoxTestGenerated.java       # ~50+ tests
│       ├── JavaUsingAstLegacyDiagnosticTestGenerated.java
│       └── JavaUsingAstResolveTestGenerated.java
├── IMPLEMENTATION_PLAN.md        # Overall architecture plan
├── AGENT_INSTRUCTIONS.md         # This file
└── FIXING_ITERATIONS.md          # Iteration prompts
```

### Test Data Location
- Tests use data from: `compiler/testData/codegen/box/javaInterop/`
- Each test has embedded Java files (marked with `// FILE: ClassName.java`)
- Tests also have Kotlin files and expected results

---

## Ground Rules for AI Agents

### Code Quality Standards
1. **Follow Kotlin repository conventions**:
   - Use existing coding style from the codebase
   - Match naming conventions in FIR and compiler modules
   - Keep files focused and cohesive
   - **MANDATORY**: Follow all rules in `.ai/guidelines.md`

2. **Minimal code philosophy**:
   - Write the minimum code necessary to solve the problem
   - Avoid speculative features or over-engineering
   - Refactor only when needed

3. **Comments** (CRITICAL - Read Carefully):
   - **Default: No comments** - Write self-explanatory code first
   - **When to comment**:
     * Non-obvious design decisions (why this approach over alternatives)
     * Workarounds for bugs (with YouTrack reference: `// Workaround for KT-XXXXX`)
     * Complex algorithms that aren't immediately clear
     * Intentional limitations or TODOs for future work
   - **NEVER comment**:
     * What the code does (if it's readable, the code itself is the comment)
     * Simple operations like "return the value" or "check if null"
     * Paraphrasing the code in English
     * Implementation details that are obvious from reading the code
   - **Examples**:
     ```kotlin
     // ❌ BAD - States the obvious
     // Return the class names if we have this package
     val byName = index[packageFqName]
     if (byName != null) return byName.keys
     
     // ✅ GOOD - No comment needed, code is clear
     val byName = index[packageFqName]
     if (byName != null) return byName.keys
     
     // ❌ BAD - Explains what, not why
     // Return empty set for packages not in index
     return emptySet()
     
     // ✅ GOOD - Explains non-obvious contract
     // Empty set means "we checked, nothing found". Null means "cannot compute".
     return emptySet()
     ```
   - **Rule of thumb**: If someone reading the code would ask "why?", comment. If they would ask "what?", improve the code instead.

4. **Testing**:
   - Every fix must have a test (either unit test or box test passes)
   - Prefer isolated unit tests when possible
   - Use box tests to verify end-to-end scenarios

### Interaction Protocol
1. **Before each iteration**: Present your analysis and proposed approach
2. **Ask for confirmation**: Wait for user approval before implementing
3. **After implementation**: Report what was changed and verification results
4. **If stuck**: Ask specific questions rather than making assumptions

### Use of Tools
- **MANDATORY**: Use JetBrains IDE MCP tools for all file operations (see `.ai/guidelines.md`)
  - Use `mcp__jetbrains__get_file_text_by_path` instead of `Read`
  - Use `mcp__jetbrains__replace_text_in_file` instead of `Edit`/`Write`
  - Use `mcp__jetbrains__search_in_files_by_text` instead of `Grep`
  - Use `mcp__jetbrains__find_files_by_name_keyword` instead of `Glob`
- Use `mcp__jetbrains__get_file_problems` after changes to check for warnings
- Use `mcp__jetbrains__build_project` to verify compilation (if needed)
- Use `mcp__jetbrains__search_*` tools for code exploration
- Use default `Bash` tool (NOT `execute_terminal_command`) for terminal commands

### FIR Terminology (MANDATORY)
Use correct FIR naming conventions throughout:
- ✅ `simpleImports` (NOT `singleTypeImports`)
- ✅ `starImports` (NOT `onDemandImports`)
- Match naming conventions from FIR codebase for consistency

---

## Iteration Template

Each iteration in FIXING_ITERATIONS.md follows this structure:

### Phase 1: Analysis - FOCUS ON SINGLE REPRESENTATIVE TEST
**Goal**: Understand the root cause through ONE concrete failing test

**CRITICAL**: Do NOT analyze all failures at once. Pick ONE representative test.

**Tasks**:
1. **Select ONE failing test** (pick simplest or most common error)
   - Run ONLY this single test first
   - Read its test data file (testData/codegen/box/javaInterop/)
   - Understand what it's testing
2. **Examine this test's specific failure**
   - What is the exact error message?
   - What line of code fails?
   - What values/types are involved?
3. **Trace through code for THIS test only**
   - Add logging/debugging if needed
   - Follow execution path step by step
4. **Form hypothesis for THIS specific failure**
   - Not a general pattern - this concrete case

**Deliverable**: Analysis document with:
- Selected test name and what it tests
- Exact error message and location
- Concrete root cause hypothesis
- Proposed fix approach

**Anti-pattern to AVOID**: Running all 138 tests and trying to categorize errors. This leads to analysis paralysis.

### Phase 2: Reproduction
**Goal**: Create minimal reproducible test case for the ONE issue

**Tasks**:
1. Extract minimal failing example from the ONE box test
2. Create unit test in `compiler/java-direct/test/.../` directory
3. Verify unit test fails with SAME error
4. Simplify test case as much as possible

**Deliverable**: New unit test file demonstrating the issue

### Phase 3: Implementation
**Goal**: Fix the identified issue

**Tasks**:
1. Implement the fix in appropriate source file(s)
2. Ensure unit test now passes
3. Ensure the ONE selected box test now passes
4. **Only after single test passes**: Run broader set (10-20 related tests)
5. Check that fix doesn't break existing passing tests

**Deliverable**: 
- Modified source files
- Passing unit test
- Report showing: single test → passes, related tests → improvement count

**Anti-pattern to AVOID**: Implementing a fix and immediately running all 138 tests to see aggregate improvement. This wastes time and provides unclear feedback.

### Phase 4: Validation
**Goal**: Confirm fix is correct and complete

**Tasks**:
1. Run `mcp__jetbrains__get_file_problems` on modified files
2. Fix any warnings related to changes
3. Run full test suite ONLY to measure overall progress (not for debugging)
4. Document any limitations or partial fixes
5. **MANDATORY**: Update `ITERATION_RESULTS.md` with your findings

**Deliverable**: 
- Validation report with test results
- Updated `ITERATION_RESULTS.md` entry

## Focus Strategy - How to Pick "The ONE Test"

When starting an iteration:

### Step 1: Run Full Suite ONCE (for statistics only)
```bash
./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated" -q 2>&1 | tee test_output.txt
```

### Step 2: Categorize Error Types
```bash
# Extract unique error patterns
grep "DIAGNOSTICS:" test_output.txt | sort | uniq -c | sort -rn | head -20
```

### Step 3: Pick Most Frequent Error Pattern
Choose the error that appears most often. This represents the biggest blocker.

### Step 4: Find Simplest Test With That Error
From tests showing that error, pick the shortest/simplest one:
- Fewest lines of Java code
- Fewest features used (no generics, no annotations, etc.)
- Most focused test scenario

### Step 5: Work ONLY on That Test
```bash
# Run ONLY this test
./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.testSimpleInheritance" -q
```

### Step 6: After Fix Works, Verify Related Tests
```bash
# Find tests with similar characteristics
./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.test*Inheritance*" -q
```

### Step 7: THEN Run Full Suite (to measure progress)

## Example Focus Workflow

**BAD Approach** (what to avoid):
```
Agent: "I'll analyze all 108 failing tests..."
[Reads 108 test files]
[Tries to categorize into 15 different patterns]
[Gets overwhelmed, makes speculative changes]
[Runs all tests again, sees no improvement]
[Doesn't know which test to debug]
```

**GOOD Approach** (what to do):
```
Agent: "Running full suite to get error statistics..."
[Finds MISSING_DEPENDENCY_CLASS appears 88 times]
Agent: "Finding simplest test with MISSING_DEPENDENCY_CLASS..."
[Identifies testAbstractMethodsOfAny.kt - 20 lines, uses Object only]
Agent: "Running ONLY testAbstractMethodsOfAny..."
[Test fails: "cannot find java.lang.Object"]
Agent: "Let me trace why Object isn't found..."
[Adds logging, discovers classifierQualifiedName returns "Object" not "java.lang.Object"]
Agent: "I'll fix classifierQualifiedName to qualify java.lang names..."
[Implements fix]
Agent: "Testing ONLY testAbstractMethodsOfAny..."
[Test passes!]
Agent: "Now testing all tests with 'Object' in name..."
[Finds 10 more tests pass]
Agent: "Running full suite to measure overall improvement..."
[38/138 → 48/138, documents in ITERATION_RESULTS.md]
```

---

## Key Files to Understand

### Core Implementation
- `JavaClassFinderOverAstImpl.kt`: Entry point, file indexing, class lookup
- `JavaClassOverAst.kt`: Java class model, most complex logic here
- `JavaTypeOverAst.kt`: Type representations
- `parse.kt`: Parser integration with KMP Java parser

### FIR Integration Points
- `compiler/fir/fir-jvm/src/.../FirJavaFacade.kt`: Converts Java Model to FIR (reference)
- `compiler/fir/fir-jvm/src/.../JavaSymbolProvider.kt`: Exposes Java classes to FIR (reference)
- `compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt`: Type conversion logic (reference)

### Java Model Interfaces (implement these)
- `core/compiler.common.jvm/src/.../load/java/structure/javaElements.kt`

---

## Common Pitfalls

1. **Circular Dependencies**: FIR needs Java model, Java model needs FIR
   - **Solution**: Lazy evaluation everywhere

2. **Wrong Terminology**: Using different names than FIR codebase
   - Use `simpleImports`, `starImports` (not singleType/onDemand)
   - Use FIR naming conventions throughout

3. **Over-Engineering**: Trying to implement everything at once
   - Start minimal, iterate, add complexity gradually

4. **Not Testing**: Making changes without unit tests
   - Every feature needs a test
   - Box tests verify end-to-end, but are slow

5. **Ignoring Laziness**: Eager evaluation kills performance
   - Everything should be lazy: parsing, resolution, member building

6. **Using Wrong Tools**: Using standard file tools instead of JetBrains MCP
   - Always use MCP tools for project files (see `.ai/guidelines.md`)

---

## Useful Commands

```bash
# Run specific test class
./gradlew :compiler:java-direct:test --tests "JavaParsingTest" -q

# Run generated box tests (may take long)
./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated" -q

# Run single box test
./gradlew :compiler:java-direct:test --tests "JavaUsingAstLegacyBoxTestGenerated.testAbstractMethodsOfAny" -q

# Build java-direct module
./gradlew :compiler:java-direct:build -q

# Generate tests (after adding new test data)
./gradlew generateTests -q
```

Note: Use `-q` (quiet) flag to reduce output noise.

---

## Reference Documentation

- **IMPLEMENTATION_PLAN.md**: Overall architecture and design decisions
- **.ai/guidelines.md**: Kotlin project coding guidelines (MANDATORY reading)
- `compiler/AGENTS.md`: Compiler-specific agent guidelines
- `compiler/fir/fir-jvm/`: FIR Java integration (reference implementation)
- `compiler/javac-wrapper/`: Old javac-based implementation (reference for resolution logic)

---

## Success Metrics

After completing all iterations, we expect:

- ✅ **Test pass rate**: 50-70% of box tests passing (from current ~0%)
- ✅ **Core features working**:
  - Simple Java classes with fields and methods
  - Inheritance (single and multiple interfaces)
  - Basic generics
  - Import resolution
  - Package resolution
- ✅ **Code quality**: No warnings on modified files
- ✅ **Documentation**: Known limitations clearly documented

**Not expected to work yet** (future work beyond these iterations):
- Complex annotation arguments with constant evaluation
- All edge cases of generic types
- Full Java 21 feature support
- 100% test pass rate

---

## Quick Feedback Templates

If an agent produces code that doesn't follow guidelines, use these templates:

### Over-Commenting
```
The code has too many obvious comments. Please review AGENT_INSTRUCTIONS.md 
section on Comments and remove all comments that explain "what" rather than "why". 
Keep only comments for non-obvious design decisions.

Examples to remove:
- "Return the class names if we have this package" - obvious from code
- "Check if null and return" - obvious from code
Keep only comments like:
- "Empty set means checked but not found; null means cannot compute" - explains contract
```

### Wrong Terminology
```
Please use correct FIR terminology:
- Use `simpleImports` not `singleTypeImports`
- Use `starImports` not `onDemandImports`
```

### Not Using JetBrains MCP Tools
```
Please use JetBrains MCP tools instead of standard tools:
- Use `mcp__jetbrains__get_file_text_by_path` not `Read`
- Use `mcp__jetbrains__replace_text_in_file` not `Edit`/`Write`
See AGENT_INSTRUCTIONS.md "Use of Tools" section.
```

---

## Keeping Documentation Fresh

**For Human Reviewers**: Every 2-3 iterations, review `ITERATION_RESULTS.md` and update this file with:
- Newly discovered key files or patterns
- Updated "What Works" / "What's Failing" sections
- New common pitfalls encountered
- Adjusted success metrics

This keeps core instructions lean while preserving knowledge.

---

## Document Change Log

- 2026-02-23: Added ITERATION_RESULTS.md integration and periodic update process
- 2026-02-23: Split from ITERATIVE_FIXING_PLAN.md into separate common instructions
- 2026-02-10: Original content created in ITERATIVE_FIXING_PLAN.md
