# Kotlin/Native engineering knowledge base

Documentation for engineers working *on* the Kotlin/Native (K/N) compiler and runtime: the LLVM backend, the C++ runtime, compiler
caches, C/ObjC interop and ObjC/C export. It is written for someone who knows compilers but not this codebase, and it is organized
so you can read the overview in a few minutes and drill into only the subsystem your task touches.

User-facing documentation (how to *use* K/N) lives at https://kotlinlang.org/docs/native-overview.html. Build, test and debugging
how-tos live in [kotlin-native/HACKING.md](../../kotlin-native/HACKING.md).

## Start here

**New to the code base** (30–60 minutes):
1. [compilation-model.md](compilation-model.md) — what klibs, binaries, caches, cinterop and export are, and why the model is
   closed-world. Skim if you already use K/N.
2. [architecture.md](architecture.md) — where the code lives (with Gradle paths), module dependencies, the two compilation stages.
3. [second-stage-pipeline.md](compiler/second-stage-pipeline.md) — the driver step by step: what runs, in which order, with
   which state.
4. [glossary.md](glossary.md) — the terms that recur in code, commit messages and issues.

**Investigating a bug**: start at [debugging.md](debugging.md), which maps symptoms to subsystems, flags and tests, then open the
subsystem page it points to. Each subsystem page ends with an "Investigating" section listing the relevant tests and dumps.

**Making a change**: read the subsystem page's "Invariants" and "Making changes" sections first; they list what else must change
when you touch that subsystem. Before changing anything that other artifacts depend on (klibs, caches, platform libraries,
defaults, flags), read [compatibility-and-migration.md](build-and-tooling/compatibility-and-migration.md).

## Pages by area

Pages are grouped into one directory per area. Two kinds of pages are mixed in each: *subsystem pages* (role, concepts, main
flows, invariants, extension points, how to investigate, open questions; marked "Read it when you work on") and *deep dives*,
single documents that tie one complex system together: the algorithm from the core idea to the details, data structures,
flags, tests, trade-offs, connections to other subsystems, and the implied contracts other code relies on.

### Overview

The shared vocabulary and the code map; read these first.

| Page | What it covers |
|------|----------------|
| [compilation-model.md](compilation-model.md) | How klibs, binaries, caches, interop and export fit together from the outside. *Read it when you work on:* anything; it is the shared vocabulary |
| [architecture.md](architecture.md) | Where is the code, which module depends on which, what runs when the compiler is invoked. *Read it when you work on:* orientation, build layout, distribution |

### Compiler: pipeline and lowerings ([compiler/](compiler/))

The second-stage driver, klib loading, the IR lowerings and the language features implemented by them.

| Page | What it covers |
|------|----------------|
| [second-stage-pipeline.md](compiler/second-stage-pipeline.md) | The klib → binary driver: output kinds, fragments, phase contexts, dump/disable flags. *Read it when you work on:* compiler crashes, adding a phase, "where does X run" |
| [lowerings.md](compiler/lowerings.md) | The IR lowering pipeline, synchronization points, per-file invariant, Native-specific lowerings. *Read it when you work on:* wrong code for a language feature, IR validation failures |
| [klib-linking.md](compiler/klib-linking.md) | Library resolution, K1 descriptors, IR deserialization, lazy IR, partial linkage. *Read it when you work on:* unbound symbols, klib version errors, PL stubs |
| [static-initializers.md](compiler/static-initializers.md) | Global and class initialization end to end: lowering, state machine, eager/lazy, caches, redundant-call removal |
| [coroutines.md](compiler/coroutines.md) | Suspend functions: tail-call classification, the generated state-machine class, late variable spilling, `indirectbr` codegen, what the GC and escape analysis see, the stdlib contract |
| [intrinsics.md](compiler/intrinsics.md) | The `kotlin.native.internal` intrinsics and compiler-facing annotations as a set: who reads each, how a call is resolved, the two call directions between Kotlin and the runtime, how to add one |

### Optimizations ([optimizations/](optimizations/))

The whole-program analyses of the second stage. Start at the overview, then the deep dive for the analysis at hand.

| Page | What it covers |
|------|----------------|
| [optimizations/README.md](optimizations/README.md) | Overview of the whole-program analyses, their order and gating. *Read it when you work on:* `-opt`-only bugs; start here, then the deep dives below |
| [dataflow-ir.md](optimizations/dataflow-ir.md) | The DataFlowIR model and builder shared by all whole-program analyses; the call graph |
| [devirtualization.md](optimizations/devirtualization.md) | Variable type analysis over the constraint graph, condensation, propagation, the IR rewrite |
| [escape-analysis.md](optimizations/escape-analysis.md) | Interprocedural points-to analysis, `@Escapes`/`@PointsTo`, lifetimes, stack allocation and the GC contract |
| [dce-and-gha.md](optimizations/dce-and-gha.md) | Dead-code elimination and the root set; class intervals and interface coloring for O(1) type checks and dispatch |
| [pre-codegen-inliner.md](optimizations/pre-codegen-inliner.md) | The IR-level optimization inliner, its candidate selection and interactions |
| [casts-optimization.md](optimizations/casts-optimization.md) | Type checks and casts: `ComputeTypesPass`, `CastsOptimization`, generic safe casts, bridges, runtime checks |

### Code generation ([codegen/](codegen/))

IR → LLVM IR, the LLVM pass pipeline, object files, linking and debug info.

| Page | What it covers |
|------|----------------|
| [llvm-codegen.md](codegen/llvm-codegen.md) | IR → LLVM IR, runtime bitcode linking, LLVM pipeline, object files, linker. *Read it when you work on:* LLVM verifier errors, linker errors, debug info |
| [debug-info.md](codegen/debug-info.md) | DWARF emission (scopes, `inlinedAt`, variables, types), `ExtendedTypeInfo` and the runtime debug helpers, `konan_lldb.py`, symbolication, dSYM and linking |
| [llvm-passes.md](codegen/llvm-passes.md) | The LLVM pass pipeline per optimization mode, the custom `kotlin-*` passes, runtime inlining, function attributes codegen emits, debugging with `opt` |

### Runtime ([runtime/](runtime/))

The C++ runtime and the ABI it shares with generated code. Start at the overview, then gc.md / allocator.md.

| Page | What it covers |
|------|----------------|
| [runtime/README.md](runtime/README.md) | The C++ runtime: lifecycle, threads, suspension, roots, logging; overview of GC and allocation. *Read it when you work on:* runtime asserts, hangs; start here, then gc.md / allocator.md |
| [compiler-runtime-contract.md](runtime/compiler-runtime-contract.md) | The ABI shared by generated code and the runtime: object layout, TypeInfo, entry points, thread states, roots. *Read it when you work on:* any change that touches both `llvm/` and `runtime/` |
| [gc.md](runtime/gc.md) | The collectors (CMS, PMCS, STMS, noop): phases, barriers, termination, roots, weak processing; the scheduler |
| [allocator.md](runtime/allocator.md) | The custom allocator: pages, size classes, sweep protocol, finalizer queues, accounting, options |
| [exceptions.md](runtime/exceptions.md) | Throw, C++ unwinding and the shadow stack, landing pads, interop filtering, ObjC/Swift `NSError` bridging, unhandled exceptions and termination, stack-trace symbolication |

### Interop and export ([interop/](interop/))

C/ObjC interop, platform libraries, the commonizer and exporting Kotlin to ObjC, C and Swift.

| Page | What it covers |
|------|----------------|
| [cinterop.md](interop/cinterop.md) | The cinterop tool, cinterop klibs, interop lowerings and C stubs, ObjC interop codegen. *Read it when you work on:* C/ObjC interop bugs, `.def` files |
| [export.md](interop/export.md) | ObjC frameworks and C libraries: header generation, code spec, bridges, runtime side; pointer to Swift export. *Read it when you work on:* exported API shape, bridge crashes, NSError |
| [platform-libraries.md](interop/platform-libraries.md) | How platform libraries are built, shipped, discovered, cached and commonized, and what depends on their shape |
| [commonizer.md](interop/commonizer.md) | The klib commonizer: CIR, the merged node tree, per-declaration commonizers, type-alias substitution, optimistic numbers, hierarchical runs, KGP tasks and caching |

### Build, caches and tooling ([build-and-tooling/](build-and-tooling/))

Caches and incremental compilation, targets and toolchains, compatibility, the tools that drive the compiler, the test infrastructure.

| Page | What it covers |
|------|----------------|
| [caches.md](build-and-tooling/caches.md) | Compiler caches: building, per-file caches, dependency tracking, cache ABI, prelink. *Read it when you work on:* behaviour that differs with caches, stale caches |
| [incremental-compilation.md](build-and-tooling/incremental-compilation.md) | Incremental second-stage compilation from KGP to the relinked binary: dirty-file analysis, per-file caches |
| [compatibility-and-migration.md](build-and-tooling/compatibility-and-migration.md) | Every compatibility surface (klib ABI, stdlib/platform libs, caches, options, targets, toolchains, exported API, CLI/KGP), what enforces it, how defaults are migrated |
| [tooling-integration.md](build-and-tooling/tooling-integration.md) | What KGP, the IDE / Analysis API, the commonizer and the CLI tools pass to and expect from the compiler |
| [targets-and-toolchains.md](build-and-tooling/targets-and-toolchains.md) | `KonanTarget` and host support, `konan.properties`, the dependency downloader, Clang/linker drivers, Xcode coupling, the distribution layout, adding or removing a target |
| [test-infrastructure.md](build-and-tooling/test-infrastructure.md) | How `native/native.tests` works inside: `-Pkn.*` → settings → flags, the life of one blackbox test, grouping, the test runner protocol, directives, FileCheck, cache modes, test generation |

## Navigation

| Page | Use |
|------|-----|
| [debugging.md](debugging.md) | Symptom → subsystem → flags → tests |
| [glossary.md](glossary.md) | Definitions and pointers for K/N-specific terms |
| [status.md](status.md) | Coverage, cross-subsystem contracts, implied contracts not enforced by any check, known gaps, open questions |

Related documentation outside this directory:
[kotlin-native/HACKING.md](../../kotlin-native/HACKING.md) (building, testing, debugging, LLVM tricks),
[kotlin-native/runtime/README.md](../../kotlin-native/runtime/README.md) (runtime module map),
[kotlin-native/Interop/README.md](../../kotlin-native/Interop/README.md),
[native/native.tests/README.md](../../native/native.tests/README.md),
[docs/swift-export](../swift-export/README.md), [docs/inlining/klib-inining.md](../inlining/klib-inining.md),
[native/objcexport-header-generator/ReadMe.md](../../native/objcexport-header-generator/ReadMe.md),
[native/commonizer/README.md](../../native/commonizer/README.md), and [kotlin-native/AGENTS.md](../../kotlin-native/AGENTS.md)
for the agent-oriented summary.

## Conventions

- Pages link to source files and name the symbol, so every claim can be checked against the code. Paths are relative to the
  page's own directory.
- Each page lives in the directory of its area (see [Pages by area](#pages-by-area)); only the entry points (this README,
  compilation-model.md, architecture.md, debugging.md, glossary.md, status.md) stay at the top level. An area's overview page is
  its `README.md`. A new page goes into the area whose code it describes; add it to the area's table above and to
  [kotlin-native/AGENTS.md](../../kotlin-native/AGENTS.md).
- Statements verified against code or tests are written plainly. Anything not verified is marked **Hypothesis** or **Unverified**.
  Each page ends with an "Open questions" list; if you resolve one, update the page and remove it.
- Each concept has one canonical page; other pages link to it rather than restating it. The user-visible model is canonical in
  compilation-model.md, the module map in architecture.md, the runtime module list in kotlin-native/runtime/README.md, how to
  *run* tests in HACKING.md and how the test infrastructure *works* in build-and-tooling/test-infrastructure.md. Overview pages
  (optimizations/README.md, runtime/README.md, build-and-tooling/caches.md) summarize and link to the deep dives.
- Lines wrap at about 130 characters; tables are used for maps.
- Pictures are used wherever a flow, state machine, layout or decision tree is clearer drawn than written, but the prose stays
  complete without them. Three forms are used: Mermaid code blocks (`flowchart`, `stateDiagram-v2`, `sequenceDiagram`,
  `classDiagram`) for flows and graphs; hand-written SVG files in an `imgs/` directory next to the page, named `<page>-<topic>.svg` for layouts where
  placement carries meaning (object headers, pages, tables, timelines); ASCII blocks for bit fields and small layouts. Every
  diagram is introduced by a sentence naming the source it was derived from, and is a claim like any other: check it against the
  code and fix it when the code changes. Mermaid conventions that keep GitHub and IntelliJ rendering: quote labels that contain
  punctuation, use `<br/>` for line breaks, write angle brackets as `#lt;`/`#gt;`, no `;` inside sequence or state diagram text,
  no node named `end`. The headless parse check used while writing is `mermaid.parse` from the real library, not a renderer; a
  diagram that parses can still look wrong, so view it once.
