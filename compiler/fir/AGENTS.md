# Frontend Intermediate Representation (FIR)

Make sure to read [compiler/AGENTS.md](../AGENTS.md) for general compiler guidelines.

Read [fir-basics.md](../../docs/fir/fir-basics.md) for an understanding of the subsystem.

## FIR Compilation Phases

FIR processes code through sequential phases (see [`FirResolvePhase.kt`](tree/src/org/jetbrains/kotlin/fir/declarations/FirResolvePhase.kt)).

Key invariant: In phase B following phase A, all FIR elements visible in B are resolved to phase A.

## Inference

For type inference implementation details, read [inference.md](../../docs/fir/inference.md).

## Testing

For FIR diagnostic test data format (directives, diagnostic markers, file structure), see [testData/diagnostics/AGENTS.md](../testData/diagnostics/AGENTS.md).
