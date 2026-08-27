# SLC: missing `@IntroducedAt` compatibility overloads

## Problem

The JVM backend generates Java-visible compatibility overloads for default parameters annotated with `@IntroducedAt`. Source symbol light classes currently expose only the main declaration and some `@JvmOverloads` wrappers; they do not materialize the version compatibility overloads.

This is a general SLC gap rather than a `@JvmExposeBoxed`-specific problem. Existing test data already demonstrates it:

- [`versionOverloads/functions.java`](../testData/lightClassByPsi/versionOverloads/functions.java) contains only the full extension and suspend methods, while [`functions.lib.java`](../testData/lightClassByPsi/versionOverloads/functions.lib.java) also contains the historical overloads.
- [`versionOverloads/nonAscending.java`](../testData/lightClassByPsi/versionOverloads/nonAscending.java) omits compatibility methods, constructors, and data-class `copy` overloads present in [`nonAscending.lib.java`](../testData/lightClassByPsi/versionOverloads/nonAscending.lib.java).
- [`jvmExposeBoxed/featureInteraction/introducedAtConstructor.java`](../testData/lightClassByPsi/jvmExposeBoxed/featureInteraction/introducedAtConstructor.java) omits exposed compatibility constructors present in [`introducedAtConstructor.lib.java`](../testData/lightClassByPsi/jvmExposeBoxed/featureInteraction/introducedAtConstructor.lib.java).

The generated overloads are deliberately visible to Java. Each historical overload is annotated with Kotlin `@Deprecated(..., level = DeprecationLevel.ERROR)`, but it is not hidden as a JVM-synthetic method. Omitting it therefore causes a real Java resolution false negative.

## Backend semantics to mirror

[`VersionOverloadsLowering`](../../../compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/lower/VersionOverloadsLowering.kt) groups defaulted regular parameters by their `@IntroducedAt` version and generates one wrapper for each historical API boundary. A wrapper omits parameters introduced after that boundary and delegates to the main declaration using their default values.

On the JVM:

- versions are ordered with `MavenComparableVersion`;
- wrappers copy the original annotations and receive an exact `@Deprecated` message at `ERROR` level;
- [`JvmVersionOverloadsLowering`](../../../compiler/ir/backend.jvm/lower/src/org/jetbrains/kotlin/backend/jvm/lower/JvmVersionOverloadsLowering.kt) removes `@JvmOverloads` from version wrappers;
- version overload generation runs before [`JvmOverloadsAnnotationLowering`](../../../compiler/ir/backend.jvm/lower/src/org/jetbrains/kotlin/backend/jvm/lower/JvmOverloadsAnnotationLowering.kt);
- `@JvmOverloads` wrappers whose JVM signatures conflict with version wrappers are skipped;
- constructors and generated data-class `copy` methods participate;
- subsequent value-class lowering applies `@JvmExposeBoxed`, mangling, and boxed type mapping to the generated wrappers.

## Why it happens

[`createMethodsJvmOverloadsAware`](../src/org/jetbrains/kotlin/light/classes/symbol/classes/symbolLightClassUtils.kt) creates the full method and returns immediately unless `@JvmOverloads` is present. It has no path that creates version wrappers.

The same file already calculates historical masks in `valueParameterMaskFilter`, but uses them only to suppress conflicting `@JvmOverloads` methods. Its documentation assumes that `@IntroducedAt` methods were generated elsewhere; SLC has no such generator.

Constructors have an additional symptom. [`SymbolLightConstructor.shouldGenerateNoArgOverload`](../src/org/jetbrains/kotlin/light/classes/symbol/methods/SymbolLightConstructor.kt) rejects any all-default constructor containing `@IntroducedAt`. That avoids some clashes, but it also removes legitimate no-argument constructors such as `IntroducedAfterBase()`, where the version wrapper has descriptor `(String)V` rather than `()V`.

SLC also lacks per-light-method metadata for a generated version, so it cannot currently synthesize the backend's deprecation annotation, report `isDeprecated`, or remove `@JvmOverloads` only from version wrappers.

## Impact

- Java references to compatibility overloads fail to resolve against Kotlin source but work against the compiled library.
- Completion and overload lists change when a source dependency is replaced with its binary form.
- Constructors and data-class `copy` methods have incomplete Java PSI.
- `@JvmOverloads`, `@JvmName`, and `@JvmExposeBoxed` interactions can produce both missing declarations and incorrect conflict suppression.

## Expected behavior

For every callable with at least one defaulted `@IntroducedAt` parameter, SLC should expose the same historical wrappers as the JVM backend, with:

- the same selected parameters and Java/JVM signatures;
- the same visibility, modality, receiver, context, and suspend parameters;
- copied annotations except for the backend-specific removal of `@JvmOverloads`;
- the exact generated Kotlin `@Deprecated` annotation and deprecated PSI state;
- normal backend naming and value-class exposure rules applied after wrapper generation.

## Fix plan

1. Treat the existing `lightClassByPsi/versionOverloads` and `jvmExposeBoxed/featureInteraction` goldens as the test-first reproduction. Compare them with the compiler's `compiler/testData/codegen/boxJvm/versionOverloads` coverage and add any missing SLC inputs in a separate test-only commit.
2. Refactor the existing `@IntroducedAt` mask calculation into a reusable version-overload descriptor generator. Each descriptor should contain the selected-parameter mask and the historical version used in the deprecation message.
3. Preserve current default-value semantics, including defaults from `expect` declarations for `actual` callables, and account for generated data-class `copy` parameters.
4. Extend method creation to generate version descriptors independently of `@JvmOverloads`. Feed each descriptor through the same regular/boxed method-generation path as the main declaration.
5. Add generated-method metadata for version wrappers so their modifier list can:
   - synthesize the exact Kotlin `@Deprecated` annotation at `ERROR` level;
   - make `PsiDocCommentOwner.isDeprecated`/the corresponding PSI query return true;
   - copy source annotations while excluding `@JvmOverloads` only on these wrappers.
6. Merge version and `@JvmOverloads` candidates using actual JVM identities. When both lowerings produce the same identity, keep the version wrapper, matching backend phase order. Preserve the existing non-ascending and same-erased-type behavior.
7. Extend constructor generation to combine:
   - version compatibility constructors;
   - ordinary all-default no-argument construction;
   - regular unboxed and exposed boxed constructors.
   Resolve descriptor clashes instead of rejecting every constructor containing `@IntroducedAt`. In particular, `IntroducedOnly()` should be the deprecated version wrapper, while `IntroducedAfterBase` should have both deprecated `IntroducedAfterBase(String)` and ordinary `IntroducedAfterBase()`.
8. Apply the overload-family exposure model from [the `@JvmOverloads` propagation problem](jvm-expose-boxed-jvm-overloads-propagation.md), so version wrappers follow backend `@JvmName` and `@JvmExposeBoxed` naming and annotation rules.
9. Keep version analysis lazy and gated on a defaulted `@IntroducedAt` parameter to avoid adding work to normal member enumeration.

## Test matrix

- ordinary, extension, and suspend functions;
- constructors with only introduced parameters and with a non-introduced base;
- data-class primary constructors and generated `copy`;
- ascending, non-ascending, equal, and Maven-qualified versions;
- multiple parameters introduced at the same version;
- parameters with equal erased JVM types;
- `@JvmOverloads` collisions;
- `@JvmName`, `@JvmStatic`, internal declarations, and inner classes;
- value-class parameters and return types;
- explicit/default/implicit `@JvmExposeBoxed`;
- expect/actual default values.

## Definition of done

- Source SLC goldens contain every Java-visible version wrapper present in compiled goldens.
- Generated wrappers have backend-equivalent parameter masks, names, annotations, and deprecation state.
- `@JvmOverloads` and no-argument constructor generation introduce no duplicate JVM identities.
- Java resolve behaves identically for Kotlin source and the corresponding compiled library.

