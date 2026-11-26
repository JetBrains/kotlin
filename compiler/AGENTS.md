# Compiler Architecture

## Frontend

**K2/FIR (Current)**: Located in `compiler/fir/` - Frontend IR, the new compiler frontend

Read [AGENTS.md](fir/AGENTS.md).

## IR (Intermediate Representation)

Located in `compiler/ir/`. Backend IR is used by all targets for:
- Lowering (transforming code to target-friendly form)
- Optimization
- Serialization to klibs

Backend implementations:
- `compiler/ir/backend.jvm/` - JVM backend
- `compiler/ir/backend.js/` - JavaScript backend
- `compiler/ir/backend.wasm/` - WebAssembly backend
- `kotlin-native/backend.native/`, `native/` - Native backend

## Diagnostic Messages

When adding or changing diagnostic messages, follow the [Diagnostic Message Style Guide](../docs/diagnostic_message_style_guide.md).

## Commit Guidelines

- **Test-before-fix**: When fixing an issue and adding a test, commit the test data as a separate commit **before** the fix. The test in this commit must pass on current HEAD, i.e. it documents the current (buggy) behavior; the fix commit then updates the test data expectations. This helps reviewers see how the fix actually changes semantics (the test will show diagnostic differences in the fix commit).
