# Commit Guidelines

**BEFORE creating any commit, you MUST read `docs/code_authoring_and_core_review.md`** — it contains essential rules for commit messages, code review process, and MR structure.

IMPORTANT formatting rules:

- Reference YouTrack issues (KT-XXXXX) in commit messages when applicable.
- Use `^KT-XXXXX` in the commit body to link an issue or `^KT-XXXXX Fixed` to auto-close it.
- Keep the subject line and body under 72 characters, use imperative mood.
- Prefix the subject line with an appropriate tag for the subsystem (such as, but not exhaustive, `[FIR]`, `[K/N]`, or `[BTA]`).
  - Consult the Subsystem Tags list below for the most popular tags. If unsure which tag to use, ask the user.
- Commit messages must explain not just WHAT but also WHY and HOW.
- Commit tests together with corresponding code changes.
- Non-functional changes (refactorings, reformats) should be in separate commits.

## Subsystem Tags

This is a list of the most popular subsystem tags to be used as a subject line prefix (see the formatting rules above).

Where a tag maps to an [Area](guidelines.md#areas), the area name is given so you can read its docs.

- `FIR` — *FIR (K2 frontend)* area.
- `Tests` — *Test infrastructure* area.
- `Analysis API` — *Analysis API* area.
- `Gradle` — *Kotlin Gradle Plugin* area; general Gradle build integration.
- `K/N` — *Backend: Native* area (Kotlin/Native).
- `Build` — The Kotlin repository's own build configuration and infrastructure (Gradle build scripts, bootstrap, verification metadata).
- `IR` — *IR* area.
- `Wasm` — *Backend: WASM* area.
- `BTA` — *Build Tools API* area.
- `JS` — *Backend: JS* area.
- `Native` — *Backend: Native* area (alias of `K/N`).
- `JVM` — *Backend: JVM* area.
- `KDF` — The Kotlin DataFrame compiler plugin (under *Compiler plugins*).
- `LL` — Low-Level FIR API, part of the *Analysis API* area.
- `K/JS` — *Backend: JS* area (Kotlin/JS).
- `CLI` — Compiler command-line interface and the top-level compilation pipeline.
- `stubs` — PSI stub building for source and binary declarations (used by the *Analysis API*).
- `Reflection` — kotlin-reflect, the runtime reflection library.
- `K2` — The K2 compiler as a whole (FIR-based frontend and related machinery).
- `FE` — K2/FIR frontend work, especially type inference, call resolution, and checkers (a finer-grained sibling of `FIR`).
- `Swift Export` — Generating Swift API from Kotlin for Apple interop (Kotlin/Native).
- `Lombok` — The Lombok compiler plugin (under *Compiler plugins*).
- `Maven` — The Kotlin Maven plugin and Maven build integration.
- `PL` — Partial linkage in the KLIB/IR linker.
- `PSI` — *PSI* area.
- `Klib` — The KLIB format and Kotlin library artifacts (Native/JS/Wasm).
- `FIR2IR` — The fir2ir phase that lowers FIR to IR (part of the *FIR (K2 frontend)* area).
- `SLC` — Symbol Light Classes, part of the *Analysis API* area.
- `KGP` — *Kotlin Gradle Plugin* area.
- `stdlib` — *Standard library* area.
- `Scripting` — Kotlin scripting support (`.kts` files and the scripting API).
- `decompiler` — The Kotlin binary (`.class`/metadata) decompiler and cls stub builder (used by the *Analysis API*).
- `SSoT` — Single source of truth for compiler arguments, shared by the CLI and Build Tools API.
- `LC` — Light Classes, part of the *Analysis API* area.
- `Compose` — The Jetpack Compose compiler plugin (under *Compiler plugins*).
- `PowerAssert` — The power-assert compiler plugin (under *Compiler plugins*).
- `ObjCExport` — Objective-C/Swift header export in Kotlin/Native.
- `ABI Validation` — Tooling that tracks public API/ABI changes for compatibility.
