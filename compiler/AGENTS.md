# Compiler Architecture

## Intro

Consider reading [fir-basics.md](../docs/fir/fir-basics.md).

## Two Frontends

1. **K1/FE 1.0 (Legacy)**: Located in `compiler/frontend/` - uses PSI and BindingContext
2. **K2/FIR (Current)**: Located in `compiler/fir/` - Frontend IR, the new compiler frontend

## FIR Compilation Phases

FIR processes code through sequential phases (see `FirResolvePhase.kt`).

Key invariant: In phase B following phase A, all FIR elements visible in B are resolved to phase A.

## IR (Intermediate Representation)

Located in `compiler/ir/`. Backend IR is used by all targets for:
- Lowering (transforming code to target-friendly form)
- Optimization
- Serialization to klibs

Backend implementations:
- `compiler/ir/backend.jvm/` - JVM backend
- `compiler/ir/backend.js/` - JavaScript backend
- `compiler/ir/backend.wasm/` - WebAssembly backend
- `kotlin-native/backend.native/` - Native backend

## Inference

For type inference implementation details, read [inference.md](../docs/fir/inference.md).

## Commit Guidelines

- **FIR prefix**: When changes are mostly related to FIR (`compiler/fir/`), use `FIR: ` prefix in the commit subject line.
- **Test-before-fix**: When fixing an issue and adding a test, commit the test data as a separate commit **before** the fix. This helps reviewers see how the fix actually changes semantics (the test will show diagnostic differences in the fix commit).

## Testing

For FIR analysis test data format (directives, diagnostic markers, file structure), see [analysis-tests/AGENTS.md](fir/analysis-tests/AGENTS.md).