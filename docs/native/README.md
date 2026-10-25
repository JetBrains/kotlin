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
3. [second-stage-pipeline.md](second-stage-pipeline.md) — the driver step by step: what runs, in which order, with which state.
4. [glossary.md](glossary.md) — the terms that recur in code, commit messages and issues.

**Investigating a bug**: start at [debugging.md](debugging.md), which maps symptoms to subsystems, flags and tests, then open the
subsystem page it points to. Each subsystem page ends with an "Investigating" section listing the relevant tests and dumps.

**Making a change**: read the subsystem page's "Invariants" and "Making changes" sections first; they list what else must change
when you touch that subsystem. Before changing anything that other artifacts depend on (klibs, caches, platform libraries,
defaults, flags), read [compatibility-and-migration.md](compatibility-and-migration.md).

## Subsystem pages

One page per subsystem: role, concepts, main flows, invariants, extension points, how to investigate, open questions.

| Page | What it answers | Read it when you work on… |
|------|-----------------|---------------------------|
| [compilation-model.md](compilation-model.md) | How klibs, binaries, caches, interop and export fit together from the outside | anything; it is the shared vocabulary |
| [architecture.md](architecture.md) | Where is the code, which module depends on which, what runs when the compiler is invoked | orientation, build layout, distribution |
| [second-stage-pipeline.md](second-stage-pipeline.md) | The klib → binary driver: output kinds, fragments, phase contexts, dump/disable flags | compiler crashes, adding a phase, "where does X run" |
| [lowerings.md](lowerings.md) | The IR lowering pipeline, synchronization points, per-file invariant, Native-specific lowerings | wrong code for a language feature, IR validation failures |
| [optimizations.md](optimizations.md) | Overview of the whole-program analyses, their order and gating | `-opt`-only bugs; start here, then the deep dives below |
| [llvm-codegen.md](llvm-codegen.md) | IR → LLVM IR, runtime bitcode linking, LLVM pipeline, object files, linker | LLVM verifier errors, linker errors, debug info |
| [compiler-runtime-contract.md](compiler-runtime-contract.md) | The ABI shared by generated code and the runtime: object layout, TypeInfo, entry points, thread states, roots | any change that touches both `llvm/` and `runtime/` |
| [runtime.md](runtime.md) | The C++ runtime: lifecycle, threads, suspension, roots, logging; overview of GC and allocation | runtime asserts, hangs; start here, then gc.md / allocator.md |
| [caches.md](caches.md) | Compiler caches: building, per-file caches, dependency tracking, cache ABI, prelink | behaviour that differs with caches, stale caches |
| [klib-linking.md](klib-linking.md) | Library resolution, K1 descriptors, IR deserialization, lazy IR, partial linkage | unbound symbols, klib version errors, PL stubs |
| [cinterop.md](cinterop.md) | The cinterop tool, cinterop klibs, interop lowerings and C stubs, ObjC interop codegen | C/ObjC interop bugs, `.def` files |
| [export.md](export.md) | ObjC frameworks and C libraries: header generation, code spec, bridges, runtime side; pointer to Swift export | exported API shape, bridge crashes, NSError |

## Deep dives

Single documents that tie one complex system together: the algorithm from the core idea to the details, data structures,
flags, tests, trade-offs, connections to other subsystems, and the implied contracts other code relies on.

| Page | System |
|------|--------|
| [dataflow-ir.md](dataflow-ir.md) | The DataFlowIR model and builder shared by all whole-program analyses; the call graph |
| [devirtualization.md](devirtualization.md) | Variable type analysis over the constraint graph, condensation, propagation, the IR rewrite |
| [escape-analysis.md](escape-analysis.md) | Interprocedural points-to analysis, `@Escapes`/`@PointsTo`, lifetimes, stack allocation and the GC contract |
| [dce-and-gha.md](dce-and-gha.md) | Dead-code elimination and the root set; class intervals and interface coloring for O(1) type checks and dispatch |
| [pre-codegen-inliner.md](pre-codegen-inliner.md) | The IR-level optimization inliner, its candidate selection and interactions |
| [static-initializers.md](static-initializers.md) | Global and class initialization end to end: lowering, state machine, eager/lazy, caches, redundant-call removal |
| [casts-optimization.md](casts-optimization.md) | Type checks and casts: `ComputeTypesPass`, `CastsOptimization`, generic safe casts, bridges, runtime checks |
| [incremental-compilation.md](incremental-compilation.md) | Incremental second-stage compilation from KGP to the relinked binary: dirty-file analysis, per-file caches |
| [gc.md](gc.md) | The collectors (CMS, PMCS, STMS, noop): phases, barriers, termination, roots, weak processing; the scheduler |
| [allocator.md](allocator.md) | The custom allocator: pages, size classes, sweep protocol, finalizer queues, accounting, options |
| [platform-libraries.md](platform-libraries.md) | How platform libraries are built, shipped, discovered, cached and commonized, and what depends on their shape |
| [compatibility-and-migration.md](compatibility-and-migration.md) | Every compatibility surface (klib ABI, stdlib/platform libs, caches, options, targets, toolchains, exported API, CLI/KGP), what enforces it, how defaults are migrated |
| [tooling-integration.md](tooling-integration.md) | What KGP, the IDE / Analysis API, the commonizer and the CLI tools pass to and expect from the compiler |

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

- Pages link to source files and name the symbol, so every claim can be checked against the code. Paths are relative to this
  directory.
- Statements verified against code or tests are written plainly. Anything not verified is marked **Hypothesis** or **Unverified**.
  Each page ends with an "Open questions" list; if you resolve one, update the page and remove it.
- Each concept has one canonical page; other pages link to it rather than restating it. The user-visible model is canonical in
  compilation-model.md, the module map in architecture.md, the runtime module list in the runtime README, and test mechanics in
  HACKING.md. Overview pages (optimizations.md, runtime.md, caches.md) summarize and link to the deep dives.
- Lines wrap at about 130 characters; tables are used for maps and mermaid for flows only where a picture clarifies.
