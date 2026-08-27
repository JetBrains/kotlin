# SLC: regular and boxed callables collide on the same JVM signature

## Problem

Symbol light classes can materialize two Java PSI declarations for one Kotlin callable:

- the regular JVM declaration, which may use unboxed value-class types or a mangled name;
- the Java-facing declaration requested by `@JvmExposeBoxed`, which uses boxed value-class types and an exposed name.

Usually these declarations have different JVM signatures. However, boxing does not always change the mapped parameter types. If the exposed name is also the regular name, the two declarations have the same erased JVM signature. The JVM backend emits only the exposed declaration in this case, while SLC currently emits both.

The clearest method reproducer is [`resultNullable.kt`](../testData/lightClassByPsi/jvmExposeBoxed/resultNullable.kt):

- `consume(Result<String>?)`;
- `consume1<T : Result<String>?>(T)`;
- `consume2<T : Result<String>>(T?)`.

For each function, the source SLC golden [`resultNullable.java`](../testData/lightClassByPsi/jvmExposeBoxed/resultNullable.java) contains an annotated exposed method and an unannotated regular method with the same erased signature. The compiled golden [`resultNullable.lib.java`](../testData/lightClassByPsi/jvmExposeBoxed/resultNullable.lib.java) contains only the annotated method.

Constructors have the same problem. For `Test(StringWrapper?)` in [`constructorBoxed.kt`](../testData/lightClassByPsi/jvmExposeBoxed/constructorBoxed.kt), SLC produces both a public exposed constructor and a private regular constructor with descriptor `(StringWrapper)V`. The backend produces only the public exposed constructor.

This is not a general instruction to remove regular counterparts. Both declarations must remain when their JVM identities differ, for example when:

- the exposed declaration has a custom name;
- regular and boxed value-class parameter representations differ;
- the regular declaration has a mangled name.

## Why it happens

[`methodGeneration`](../src/org/jetbrains/kotlin/light/classes/symbol/methods/symbolLightMethodUtils.kt) decides independently whether a regular method and a boxed method are required. It reasons about value classes, mangling, visibility, and exposure mode, but it does not compare the final JVM signatures.

[`SymbolLightSimpleMethod.createSimpleMethods`](../src/org/jetbrains/kotlin/light/classes/symbol/methods/SymbolLightSimpleMethod.kt) then appends both requested methods. [`SymbolLightConstructor.createConstructors`](../src/org/jetbrains/kotlin/light/classes/symbol/methods/SymbolLightConstructor.kt) similarly appends an exposed constructor and a regular constructor without checking whether they map to the same descriptor.

A source-level check such as “the parameter is nullable” is insufficient. The collision depends on the final Java name and the erased JVM parameter types after applying the regular or boxed type-mapping mode.

## Impact

SLC presents Java resolve with a method set that cannot exist in the compiled class:

- method lookup may see duplicate candidates with the same erased signature;
- constructor enumeration includes a spurious private overload;
- completion, inspections, and signature-based caches can disagree with compiled Kotlin;
- retaining the wrong candidate can also expose incorrect generic signature information for value-class-bounded type parameters.

## Expected behavior

For each regular/exposed pair:

1. Compute the Java name and erased JVM parameter descriptor for both candidates.
2. If the JVM identities differ, keep the declarations selected by the existing generation rules.
3. If the identities are equal, emit only the backend-equivalent exposed declaration.
4. The surviving declaration must have the backend's visibility, annotations, boxed parameter mapping, and generic signature.

Return types must not be part of the collision key because they are not part of a JVM method signature.

## Fix plan

1. Use the existing `resultNullable` and `constructorBoxed` source-versus-library goldens as the test-first reproduction. Add any missing accessor, `@JvmName`, or implicit-directive cases in a separate test-only commit before changing the implementation.
2. Introduce a small JVM identity abstraction for generated light methods: Java name plus erased parameter types. It must use the same parameter selection and type-mapping modes as the actual regular and exposed candidates.
3. In `SymbolLightSimpleMethod.createSimpleMethods`, perform the identity comparison only when both variants are requested. Avoid eagerly mapping signatures on the common one-method path.
4. Apply the same rule to constructors in `SymbolLightConstructor.createConstructors`. Constructor visibility is not sufficient to distinguish JVM declarations.
5. Prefer the exposed candidate on collision. Verify that value-class-bounded type parameters are rendered like the compiled exposed bridge rather than merely deleting the duplicate regular method.
6. Reuse the same identity comparison for overload-family collision handling where practical, instead of maintaining separate approximations based only on source class IDs.
7. Update the source SLC goldens and leave the `.lib.java` files as the backend source of truth.

## Test matrix

- nullable `Result` parameter;
- type parameter whose upper bound is nullable `Result`;
- nullable type parameter whose upper bound is non-null `Result`;
- nullable user-defined value-class constructor parameter;
- custom exposed name, where both differently named methods must remain;
- implicit `-Xjvm-expose-boxed`/directive variant;
- any property accessor found by the audit to produce an identical regular/exposed identity.

The relevant SLC test-data path should be run through `updateTestData`, followed by `checkTestData` for verification.

## Definition of done

- No source SLC class contains two regular/exposed declarations with the same JVM identity.
- All Java-visible declarations in the affected source goldens match the compiled goldens.
- Legitimate mangled, differently named, or differently typed counterparts remain present.
- Java resolution over the surviving method or constructor is unambiguous.

