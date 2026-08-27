# SLC: `@JvmOverloads` loses `@JvmExposeBoxed` naming and annotation state

## Problem

When `@JvmOverloads` removes the last value-class parameter from an overload, SLC reclassifies that overload as an ordinary method. This can change its Java name and remove its `@JvmExposeBoxed` annotation even though the JVM backend keeps the overload in the exposed family.

The focused reproducer is [`jvmOverloadsValueParameter.kt`](../testData/lightClassByPsi/jvmExposeBoxed/featureInteraction/jvmOverloadsValueParameter.kt):

```kotlin
@JvmExposeBoxed("bar")
@JvmOverloads
fun foo(o: String = "O", k: StringWrapper = StringWrapper("K")): String
```

The current source golden emits `bar(String, StringWrapper)`, but emits the shorter overloads as `foo()` and `foo(String)`. The compiled golden emits `bar()`, `bar(String)`, and `bar(String, StringWrapper)`, all carrying `@JvmExposeBoxed("bar")`.

The default-name case has the right Java method names by accident, but still loses the annotation on overloads that no longer contain a value class. The interaction with `@JvmName` has another backend rule: a no-value-class overload uses the `@JvmName` name, while retaining the `@JvmExposeBoxed` annotation.

| Overload after default parameters are removed | JVM backend | Current SLC |
|---|---|---|
| No value class, explicit exposed name, no `@JvmName` | Exposed name and `@JvmExposeBoxed` | Kotlin name, no `@JvmExposeBoxed` |
| No value class, default exposed name | Kotlin name and `@JvmExposeBoxed` | Kotlin name, no `@JvmExposeBoxed` |
| No value class, `@JvmName` present | `@JvmName` name; both annotations retained | Name is generally correct; `@JvmExposeBoxed` is removed |
| Value class still present | Boxed exposed overload, plus any backend-required regular counterpart | Generally correct |

The same mismatch is visible in [`versionOverloads.java`](../testData/lightClassByPsi/jvmExposeBoxed/featureInteraction/versionOverloads.java) versus [`versionOverloads.lib.java`](../testData/lightClassByPsi/jvmExposeBoxed/featureInteraction/versionOverloads.lib.java).

## Why it happens

[`createMethodsJvmOverloadsAware`](../src/org/jetbrains/kotlin/light/classes/symbol/classes/symbolLightClassUtils.kt) invokes method generation separately for every selected parameter mask. [`methodGeneration`](../src/org/jetbrains/kotlin/light/classes/symbol/methods/symbolLightMethodUtils.kt) only requests an exposed method when the selected signature is still affected by a value class.

Once an overload drops all value-class parameters, SLC creates it with `isJvmExposedBoxed = false`. That single flag currently controls three distinct concerns:

- whether value-class types use boxed mapping;
- whether the Java name comes from `@JvmExposeBoxed`;
- whether the light method retains or synthesizes the `@JvmExposeBoxed` annotation.

Those concerns are not equivalent for generated overloads. In particular, an overload can use ordinary parameter mapping and a `@JvmName` name while still carrying `@JvmExposeBoxed`.

The JVM backend generates `@JvmOverloads` wrappers by copying annotations from the original function, then processes value-class exposure. SLC currently makes the decision only from each wrapper's remaining parameter types and loses the declaration-family context.

## Impact

- Java source resolves methods that do not exist in bytecode, such as `foo()`.
- Java source cannot resolve the actual exposed overload, such as `bar()`.
- PSI annotation queries disagree with the compiled declaration even when the Java name happens to match.
- The behavior changes depending on which default parameters are selected, so completion can present one logical overload family under two unrelated names.

## Expected behavior

SLC must reproduce the backend's overload-family semantics:

1. Exposure is inherited from the original annotated callable by every `@JvmOverloads` wrapper.
2. Whether a particular wrapper needs boxed type mapping remains mask-specific.
3. If a wrapper is value-class-affected, the boxed bridge uses the exposed name and excludes `@JvmName`, matching the existing boxed-method behavior.
4. If a wrapper is no longer value-class-affected:
   - an applicable `@JvmName` determines the Java name;
   - otherwise an explicit `@JvmExposeBoxed` name determines the Java name;
   - the `@JvmExposeBoxed` annotation remains visible in either case.
5. Regular mangled or unboxed counterparts are retained only where the backend emits them.

## Fix plan

1. Keep the existing `jvmOverloadsValueParameter*`, default-name, and `versionOverloads` goldens as the test-first reproductions. Add missing return-value-class and `@JvmName` permutations in a separate test-only commit if SLC does not already cover them.
2. Compute exposure metadata once from the original callable before iterating over `@JvmOverloads` parameter masks.
3. Split the current `isJvmExposedBoxed` decision into independent method-generation properties. At minimum, model:
   - regular versus boxed value-class type mapping;
   - the selected Java naming policy;
   - whether `@JvmExposeBoxed` and `@JvmName` should be present.
4. Pass the overload-family metadata and the mask-specific value-class state into `SymbolLightSimpleMethod` creation. Do not infer annotation retention solely from boxed type mapping.
5. Mirror the backend name-precedence matrix for explicit exposed names, default exposed names, and `@JvmName`.
6. Run the final candidates through the JVM identity collision rule described in [the identical-signature problem](jvm-expose-boxed-identical-signature-collisions.md).
7. Update source SLC goldens and compare every affected overload family with its `.lib.java` counterpart.

## Test matrix

- value-class parameter first and last in the default-parameter list;
- overload that retains a value-class parameter;
- overload that drops all value-class parameters;
- explicit and default `@JvmExposeBoxed` names;
- with and without `@JvmName`;
- value class in the return type, where regular and boxed overload families can both be required;
- member and top-level functions;
- explicit annotation and implicit directive modes, including confirmation that implicit mode does not invent an explicit exposed name after all value-class positions disappear;
- interaction with `@IntroducedAt`, after version-overload synthesis is implemented.

## Definition of done

- Source SLC and compiled goldens expose the same Java names for every generated overload.
- Generated overloads carry the same `@JvmExposeBoxed`/`@JvmName` annotations as the backend.
- No spurious overload remains under the original Kotlin name.
- Existing regular unboxed or mangled overloads are not removed when the backend retains them.
