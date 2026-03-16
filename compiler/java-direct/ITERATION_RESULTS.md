# Java-Direct: Iteration Results Log

**Current status**: See `FIXING_ITERATIONS.md` for test counts and remaining work.

**Last Updated**: 2026-03-16 (iter 37)

---

## Archives

| Archive | Iterations | Result |
|---------|-----------|--------|
| `implDocs/archive/ITERATIONS_1_6_DETAILS.md` | 1–6 | 0 → 90/138 box (65.2%) |
| `implDocs/archive/ITERATIONS_7_16_DETAILS.md` | 7–16 | 90 → 1075/1166 box (92.2%) |
| `implDocs/archive/ITERATIONS_17_23_DETAILS.md` | 17–23 | 1075 → 1150/1167 box, 1374/1442 phased (95.3%) |
| `implDocs/archive/ITERATIONS_24_26_DETAILS.md` | 24–26 | 1150/1167 → same, phased 300 → 1374/1442 |
| `implDocs/archive/ITERATIONS_27_36_DETAILS.md` | 27–36 | 1150/1167 → 1157/1168 box, **79 combined failing** |

---

## Iteration 37: Implicit Java Modifiers Area Audit — 2026-03-16

### Approach
Reference-first area audit: compared `JavaClassOverAst` and `JavaMemberOverAst` against `TreeBasedClass`, `TreeBasedField`, `TreeBasedMethod`, `TreeBasedConstructor` (javac-wrapper).

### Gaps Found and Fixed

| Gap | Reference behavior | java-direct before | Fix |
|-----|-------------------|-------------------|-----|
| `JavaClassOverAst.isFinal` | `isEnum \|\| isFinal` | only `isFinal` | Add `isEnum &&` guard (not final if has abstract methods) |
| `JavaClassOverAst.isAbstract` | `isAbstract \|\| (isAnnotationType\|isEnum) && methods.any { abstract }` | `isAbstract \|\| isInterface` | Add enum/annotation abstract method case |
| `JavaClassOverAst.findAnnotation` | `annotations.find {...}` | **always `null`** | Delegate to `annotations` collection |
| `JavaMethodOverAst.isNative` | `modifiers.isNative` | **always `false`** | `hasModifier("NATIVE_KEYWORD")` |
| `JavaConstructorOverAst.isFinal/isAbstract/isStatic` | `true/false/false` | inherited (wrong) | Override explicitly |
| `isDeprecatedInJavaDoc` (all elements) | check Javadoc comment | **always `false`** | `DOC_COMMENT` child text contains `@deprecated` |

### Unit Tests Added (`JavaParsingTest.kt`)
- `testEnumImplicitFinal` — plain enum is final, enum with abstract methods is not
- `testAnnotationTypeImplicitAbstract` — annotation with methods is abstract
- `testFindAnnotationOnClass` — findAnnotation finds/misses correctly on class
- `testNativeMethod` — isNative true/false
- `testConstructorImplicitFinal` — isFinal/isAbstract/isStatic on constructor
- `testDeprecatedInJavaDoc` — class, method, field with/without Javadoc @deprecated

### Test Results
- **Box tests**: 11 → 8 (+3 fixed: `testJavaMutableListThroughKotlin`, `testJavaAnnotationConstructorTypes`, `testJavaVisibility`)
- **Phased tests**: 68 → 66 (+2 fixed: various deprecated/javadoc tests)
- **Total failures**: 79 → 74 (5 tests fixed)
- PSI regression tests: All passing ✅

### Files Modified
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassOverAst.kt` — `isFinal`, `isAbstract`, `findAnnotation`, `isDeprecatedInJavaDoc`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt` — `isDeprecatedInJavaDoc`, `isNative`, `hasModifier` protected; `JavaConstructorOverAst` overrides
- `compiler/java-direct/test/org/jetbrains/kotlin/java/direct/JavaParsingTest.kt` — 6 new unit tests

### Key Learning
All 6 gaps shared the same root cause: implicit Java modifiers/behaviors not captured in java-direct. The area audit found all of them in one pass vs. finding them individually through failing tests.

---

## Key Architectural Patterns

### 1. Callback Pattern for Resolution
Used throughout for resolution without FirSession access in Java Model:
- `JavaClassifierType.resolve(tryResolve)` — Type resolution
- `JavaAnnotation.resolveAnnotation(tryResolve)` — Annotation resolution
- `JavaEnumValueAnnotationArgument.resolveEnumClass(tryResolve)` — Enum class resolution
- `JavaType.filterTypeUseAnnotations(isTypeUse)` — TYPE_USE filtering
- `JavaField.resolveInitializerValue(resolveReference)` — Constant evaluation

### 2. PSI/Java-Direct Discrimination in Shared Files
```kotlin
val isJavaDirectClass = classSource == null && origin.fromSource
```

### 3. Two-Phase Type Parameter Construction
For mutually-referencing type parameters: create all instances first, then update context with siblings.

### 4. Implicit Supertypes
- Enums → `java.lang.Enum<E>`
- Annotation types → `java.lang.annotation.Annotation`
- Classes without extends → `java.lang.Object`

### 5. Implicit Modifiers (Java Spec)
Must be applied explicitly in java-direct (no parser support):
- Interface members: `public abstract` (methods), `public static final` (fields)
- Interface nested types: `public static` (JLS 9.5)
- Enum constants: `public static final` (JLS 8.9.3)
- Protected members: `ProtectedAndPackage` not plain `Protected`
- Protected static members: `ProtectedStaticVisibility`

---

## Known Limitations

- **`testPseudoRawTypes`**: Java compilation error (custom `java.util.Collection`) — pre-existing, not java-direct specific
- **`testRawSupertypeOverride`**: Complex raw supertype inheritance — needs further investigation
- **Static imports**: Not yet supported in annotation argument resolution

---

## Future Iteration Template

```markdown
## Iteration N: [Title] - YYYY-MM-DD

### Root Cause Analysis
[Reference-first: check javac-wrapper / PSI / git show origin/master first]

### Fix
[Files modified, solution description]

### Test Results
- Box: X/1168, Phased: X/1442, Total failing: N

### Key Learnings
```
