# Spread Pack Test Matrix

## Compiler Core

| Area | Coverage |
| --- | --- |
| Parser and PSI | pack selector syntax, recovery cases, stub serialization updates |
| K1 resolution | named spread mapping, overlap handling, evaluation order, duplicate handling |
| FIR/K2 resolution | spread argument mapping, selector resolution, declaration expansion, diagnostics |
| IR/codegen | single-evaluation and source-order box tests for spread application |
| Diagnostics | invalid selectors, missing names, overlap, incompatible bindings, recursive function-pack cycles |
| Analysis API | scope-provider visibility for pack-expanded parameters from source and dependencies |

## Feature Scenarios

| Scenario | Status | Evidence |
| --- | --- | --- |
| `foo(...pack)` | Covered | box, diagnostics, resolved-call tests |
| multiple spreads with overlap | Covered | argument-order and named-override tests |
| explicit named override after spread | Covered | box and resolved-call tests |
| declaration pack from type | Covered | parser, diagnostics, scope-provider tests |
| declaration pack from function | Covered | diagnostics, scope-provider, cross-module tests |
| recursive `g.$props` detection | Covered | diagnostics and scope-provider tests |
| selector split such as `$attrs` / `$callbacks` | Covered | scope-provider and real Compose demo |
| bound source `T.$props(this)` | Covered | real Compose demo |
| filtered source `.exclude(...)` | Covered | real Compose demo |
| overload disambiguation for function packs | Covered | diagnostics and scope-provider tests |

## Cross-Module Coverage

| Scenario | Status |
| --- | --- |
| function-pack import from dependency source set | Covered |
| function-pack import from compiled dependency signature | Covered |
| explicit selector import across modules | Covered |
| recursive dependency pack diagnostics | Covered |

## Real Compose Validation

| Check | Command | Result |
| --- | --- | --- |
| compile demo | `./gradlew --dependency-verification=off --no-configuration-cache -p scratch/pack-compose-demo clean compileKotlin` | passed |
| launch desktop demo | `./gradlew --dependency-verification=off --no-configuration-cache -p scratch/pack-compose-demo run` | passed |
| visual screenshot verification | `./gradlew --dependency-verification=off --no-configuration-cache -p scratch/pack-compose-demo verifyVisualDemo` | passed |
| runtime JDK pinning | `compose.desktop.application.javaHome = <toolchain 21>` | passed |

## Manual Verification Artifacts

| Artifact | Location |
| --- | --- |
| compiler zip | `dist/kotlin-compiler-2.4.255-SNAPSHOT.zip` |
| demo screenshot | `scratch/pack-compose-demo/build/reports/visual-verification/pack-compose-demo.png` |
| run log | `/tmp/pack-compose-demo-run.log` |

## Known Gaps

| Gap | Impact |
| --- | --- |
| IDE plugin replacement is not bundled in this repo | stock IntelliJ will not parse or complete the new syntax |
| visual verification currently depends on a real desktop session | headless CI still needs a separate strategy if this moves beyond local validation |
