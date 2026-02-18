# Compiler Architecture

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
