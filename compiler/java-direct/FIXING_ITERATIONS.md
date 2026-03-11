# Java-Direct: Fixing Iterations

## Document Purpose

This document contains iteration plans for the `java-direct` module.

**Prerequisites**: Read `AGENT_INSTRUCTIONS.md` before starting any iteration  
**Status**: Iteration 20 complete — 1112/1166 box tests passing (95.4%)  
**Last Updated**: 2026-03-10

---

## How to Use This Document

1. **Before starting**: Read `AGENT_INSTRUCTIONS.md` thoroughly
2. **For each iteration**: Follow the 4-phase template (Analysis → Reproduction → Implementation → Validation)
3. **After completing**: Update `ITERATION_RESULTS.md` with findings

---

## Iterations 1-6: Foundation (Archived)

**Status**: ✅ Completed  
**Result**: 0 → 90/138 (65.2%) box tests passing  
**Archive**: `implDocs/archive/ITERATIONS_1_6_DETAILS.md`

| Iteration | Focus | Key Result |
|-----------|-------|------------|
| 1 | Initial Analysis | Fixed `hasDefaultConstructor()` |
| 2 | Type Resolution Architecture | Verified classifierQualifiedName approach |
| 3 | Import Handling | Implemented JavaImports |
| 4 | Star Import Resolution | Callback pattern + parameter parsing |
| 5 | Type Arguments | Generic type arguments + visibility |
| 6 | Hybrid JavaClassFinder | Combined source+binary class finding |

**Key Architectural Decisions**:
1. Type resolution in FIR layer (not Java Model)
2. Callback pattern for star imports: `resolve(tryResolve: (String) -> Boolean)`
3. Hybrid finder: source-first, binary-fallback

---

## Iterations 7-16: Core Implementation (Archived)

**Status**: ✅ Completed  
**Result**: 90/138 → 532/601 (88.5%) tests passing  
**Archive**: `implDocs/archive/ITERATIONS_7_16_DETAILS.md`

| Iteration | Focus | Key Result |
|-----------|-------|------------|
| 7a-c | Arrays, Imports, Type Params | Resolution context pattern, wildcards |
| 8 | Annotations/Nullability | TYPE_USE annotation handling |
| 9 | Interface Fields/Methods | Implicit modifiers (static/final/abstract) |
| 10 | Nested Interfaces/Enums | Implicit static for nested types |
| 11 | External Type Arguments | FIR null classifier branch fix |
| 12 | Fragmented Star Imports | Parser edge case handling |
| 13 | Annotation Callback | Unified resolution pattern |
| 14 | External Raw Types | ConeRawType in FIR |
| 15 | TYPE_USE Filtering | Filter non-TYPE_USE annotations |
| 16 | Raw Type Bounds | Type param scope in bounds |

> ⚠️ **Deep Context Recovery**: Only consult the archive documents if you need to understand specific implementation decisions or debug regressions. The `AGENT_INSTRUCTIONS.md` contains extracted learnings.

---

## Note on Iterations 11-16

Iterations 11-16 followed an **ad-hoc error analysis approach** rather than detailed pre-planned prompts. The workflow was:

1. Run tests and categorize failures
2. Pick the most impactful error pattern
3. Debug using exception-based inspection
4. Implement fix and verify
5. Document in ITERATION_RESULTS.md

This proved more effective than detailed upfront planning for the later iterations, as the remaining issues were interconnected and often revealed themselves during debugging.

---

## Iteration 17: Annotation Arguments

**Status**: ✅ Completed  
**Actual Impact**: +4 tests (176 → 172)  
**Priority**: HIGH  
**Complexity**: MEDIUM

### What Was Done

Implemented annotation argument value subinterfaces:
- `JavaLiteralAnnotationArgument` — for literal values ✅
- `JavaArrayAnnotationArgument` — for array initializers ✅
- `JavaEnumValueAnnotationArgument` — for enum constant references ✅
- `JavaClassObjectAnnotationArgument` — for `Foo.class` references ✅
- `JavaAnnotationAsAnnotationArgument` — for nested annotations ✅

### Why Fewer Tests Fixed Than Expected

The original analysis incorrectly estimated ~30 tests. The actual breakdown:
1. **Basic annotation arguments** — FIXED (most were already working or masked by other issues)
2. **Const val references** — NOT FIXED (2 tests: `REFERENCE_EXPRESSION` incorrectly treated as enum)
3. **Annotation method access** — NOT FIXED (2 tests: different issue - annotation instantiation)
4. **Baseline diffs** — NOT related to annotation arguments (104 tests)

### Remaining Annotation Issues

**Const Val References (2 tests)**:
- `testConstValAsAnnotationArgumentInJava`
- `testFakeJvmNameInJava`

These use `@Annotation(KOTLIN_CONST_VAL)` where the argument is a static import of a Kotlin const val. Current code treats ALL `REFERENCE_EXPRESSION` as enum values.

**Annotation Method Access (2 tests)**:
- `testJavaAnnotation`
- `testClassArrayInAnnotation`

These use annotation instantiation (`B("OK")`) and access `b.value`. This is NOT about annotation argument parsing — it's about exposing annotation interface methods as properties.

### Symptoms

```
java.lang.IllegalStateException: IR annotation has null argument.
 Annotation: CONSTRUCTOR_CALL 'public constructor <init> (value: kotlin.String)'
```

```
UNRESOLVED_REFERENCE: Unresolved reference 'value'. at b.kt:(106,111)
```

### Affected Tests

- `testAnnotationsOnJavaMembers`
- `testConstValAsAnnotationArgumentInJava`
- `testFakeJvmNameInJava`
- `testJavaAnnotation`
- `testClassArrayInAnnotation`
- Plus ~25 more annotation-related tests

### Phase 1: Analysis

**Goal**: Understand AST structure for annotation arguments

1. Find a failing test and examine its Java source:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated\$Annotations\$Instances.testJavaAnnotation" -q
   ```

2. Add debug exception to dump AST structure:
   ```kotlin
   // In JavaAnnotationOverAst.arguments getter
   val parameterList = node.findChildByType("ANNOTATION_PARAMETER_LIST")
   if (parameterList != null) {
       throw IllegalStateException("DEBUG: parameterList.dump=\n${parameterList.dump()}")
   }
   ```

3. Identify AST node types for:
   - `NAME_VALUE_PAIR` — the `name = value` construct
   - Value expression types (literals, arrays, references, nested annotations)

### Phase 2: Reference Implementation Study

**Reference**: `compiler/frontend.common.jvm/src/org/jetbrains/kotlin/load/java/structure/impl/annotationArgumentsImpl.kt`

Key patterns to replicate:
```kotlin
sealed class JavaAnnotationArgumentImpl(override val name: Name?) : JavaAnnotationArgument {
    companion object Factory {
        fun create(argument: PsiAnnotationMemberValue?, name: Name?, ...): JavaAnnotationArgument {
            // Dispatch based on argument type:
            // - PsiClassObjectAccessExpression → JavaClassObjectAnnotationArgumentImpl
            // - Enum value → JavaEnumValueAnnotationArgumentImpl  
            // - Literal → JavaLiteralAnnotationArgumentImpl
            // - PsiArrayInitializerMemberValue → JavaArrayAnnotationArgumentImpl
            // - PsiAnnotation → JavaAnnotationAsAnnotationArgumentImpl
            // - else → JavaUnknownAnnotationArgumentImpl
        }
    }
}
```

### Phase 3: Implementation

**File to modify**: `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaAnnotationOverAst.kt`

1. **Create factory function** to dispatch based on AST node type:
   ```kotlin
   private fun createAnnotationArgument(
       nameValuePair: JavaSyntaxNode,
       resolutionContext: JavaResolutionContext
   ): JavaAnnotationArgument {
       val name = nameValuePair.findChildByType("IDENTIFIER")?.let { Name.identifier(it.text) }
       val valueNode = nameValuePair.findChildByType("ANNOTATION_MEMBER_VALUE") 
           ?: nameValuePair.children.lastOrNull() // fallback for implicit value
       
       return when {
           valueNode == null -> JavaUnknownAnnotationArgumentOverAst(name)
           valueNode.hasType("LITERAL_EXPRESSION") -> JavaLiteralAnnotationArgumentOverAst(name, valueNode)
           valueNode.hasType("ARRAY_INITIALIZER_EXPRESSION") -> JavaArrayAnnotationArgumentOverAst(name, valueNode, resolutionContext)
           valueNode.hasType("REFERENCE_EXPRESSION") -> // Could be enum or class reference
               resolveReferenceArgument(name, valueNode, resolutionContext)
           valueNode.hasType("CLASS_OBJECT_ACCESS_EXPRESSION") -> JavaClassObjectAnnotationArgumentOverAst(name, valueNode, resolutionContext)
           valueNode.hasType("ANNOTATION") -> JavaAnnotationAsAnnotationArgumentOverAst(name, valueNode, resolutionContext)
           else -> JavaUnknownAnnotationArgumentOverAst(name)
       }
   }
   ```

2. **Implement `JavaLiteralAnnotationArgumentOverAst`**:
   ```kotlin
   class JavaLiteralAnnotationArgumentOverAst(
       override val name: Name?,
       private val valueNode: JavaSyntaxNode
   ) : JavaElementOverAst(valueNode), JavaLiteralAnnotationArgument {
       override val value: Any?
           get() = evaluateLiteral(valueNode)
       
       private fun evaluateLiteral(node: JavaSyntaxNode): Any? {
           val text = node.text
           return when {
               text.startsWith("\"") -> text.removeSurrounding("\"").unescape()
               text.startsWith("'") -> text.removeSurrounding("'").single()
               text == "true" -> true
               text == "false" -> false
               text.endsWith("L") || text.endsWith("l") -> text.dropLast(1).toLongOrNull()
               text.endsWith("F") || text.endsWith("f") -> text.dropLast(1).toFloatOrNull()
               text.endsWith("D") || text.endsWith("d") -> text.dropLast(1).toDoubleOrNull()
               text.contains(".") -> text.toDoubleOrNull()
               else -> text.toIntOrNull() ?: text.toLongOrNull()
           }
       }
   }
   ```

3. **Implement `JavaArrayAnnotationArgumentOverAst`**:
   ```kotlin
   class JavaArrayAnnotationArgumentOverAst(
       override val name: Name?,
       private val arrayNode: JavaSyntaxNode,
       private val resolutionContext: JavaResolutionContext
   ) : JavaElementOverAst(arrayNode), JavaArrayAnnotationArgument {
       override fun getElements(): List<JavaAnnotationArgument> {
           return arrayNode.children
               .filter { it.nodeType != "LBRACE" && it.nodeType != "RBRACE" && it.nodeType != "COMMA" }
               .map { createAnnotationArgument(/* wrap as name-value pair */) }
       }
   }
   ```

4. **Implement `JavaEnumValueAnnotationArgumentOverAst`**:
   ```kotlin
   class JavaEnumValueAnnotationArgumentOverAst(
       override val name: Name?,
       private val refNode: JavaSyntaxNode,
       private val resolutionContext: JavaResolutionContext
   ) : JavaElementOverAst(refNode), JavaEnumValueAnnotationArgument {
       override val enumClassId: ClassId?
           get() {
               // Parse "EnumClass.ENTRY" or just "ENTRY" with import resolution
               val text = refNode.text
               val lastDot = text.lastIndexOf('.')
               if (lastDot < 0) return null
               val className = text.substring(0, lastDot)
               // Resolve class name using resolutionContext
               return resolveToClassId(className, resolutionContext)
           }
       
       override val entryName: Name?
           get() {
               val text = refNode.text
               val lastDot = text.lastIndexOf('.')
               return if (lastDot >= 0) Name.identifier(text.substring(lastDot + 1))
                      else Name.identifier(text)
           }
   }
   ```

5. **Implement `JavaClassObjectAnnotationArgumentOverAst`**:
   ```kotlin
   class JavaClassObjectAnnotationArgumentOverAst(
       override val name: Name?,
       private val classObjNode: JavaSyntaxNode,
       private val resolutionContext: JavaResolutionContext
   ) : JavaElementOverAst(classObjNode), JavaClassObjectAnnotationArgument {
       override fun getReferencedType(): JavaType {
           // Parse "Foo.class" to get the type before ".class"
           val typeNode = classObjNode.findChildByType("TYPE") 
               ?: classObjNode.findChildByType("JAVA_CODE_REFERENCE")
           return JavaTypeOverAst.create(typeNode!!, resolutionContext)
       }
   }
   ```

6. **Implement `JavaAnnotationAsAnnotationArgumentOverAst`**:
   ```kotlin
   class JavaAnnotationAsAnnotationArgumentOverAst(
       override val name: Name?,
       private val annotationNode: JavaSyntaxNode,
       private val resolutionContext: JavaResolutionContext
   ) : JavaElementOverAst(annotationNode), JavaAnnotationAsAnnotationArgument {
       override fun getAnnotation(): JavaAnnotation {
           return JavaAnnotationOverAst(annotationNode, resolutionContext)
       }
   }
   ```

### Phase 4: Validation

1. Run the primary test:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated\$Annotations\$Instances.testJavaAnnotation" -q
   ```

2. Run all annotation-related tests:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated\$Annotations.*" -q
   ```

3. Run full test suite and count improvements:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated" -q 2>&1 | tail -5
   ```

### Success Criteria

- `testJavaAnnotation` passes
- `testConstValAsAnnotationArgumentInJava` passes
- No regressions in other tests
- ~25-30 test improvements expected

---

## Iteration 17b: Annotation Method Default Values

**Status**: ✅ Completed  
**Actual Impact**: +8 tests (1092/1166 → test passing)  
**Priority**: HIGH  
**Complexity**: LOW-MEDIUM

### What Was Done

Implemented annotation method default values parsing. When a Java annotation declares a method with a `default` value (e.g., `String value() default "OK"`), the java-direct module now correctly parses and returns this value via `annotationParameterDefaultValue`.

### Key Findings

1. **ANNOTATION_METHOD vs METHOD**: Annotation interface methods use `ANNOTATION_METHOD` node type, not `METHOD`. The initial fix for `annotationParameterDefaultValue` didn't work because `getMethods()` wasn't returning `ANNOTATION_METHOD` nodes.

2. **DEFAULT_KEYWORD Structure**: The AST structure for annotation method defaults is:
   ```
   ANNOTATION_METHOD
   ├── TYPE (String)
   ├── IDENTIFIER (value)
   ├── PARAMETER_LIST
   ├── DEFAULT_KEYWORD (default)
   └── LITERAL_EXPRESSION ("OK")
   ```

3. **Enum ClassId Resolution Bug**: When parsing enum values in annotation defaults (e.g., `MyEnum.FIRST`), the `enumClassId` was incorrectly resolving to `java.lang.MyEnum` instead of the correct package. Fixed by assuming same package for unqualified enum class names.

4. **Enum Constants Not Exposed**: `JavaClassOverAst.fields` only returned `FIELD` nodes, missing `ENUM_CONSTANT` nodes. This prevented FIR from finding enum entries when resolving annotation default values.

5. **Enum Entry Type**: `JavaFieldOverAst.type` used `createJavaType()` which doesn't work for `ENUM_CONSTANT` nodes. Created new `JavaClassifierTypeForEnumEntry` class that returns the containing enum class as the type.

6. **WHITE_SPACE in Annotation Arguments**: The `createAnnotationArgument` function wasn't filtering `WHITE_SPACE` nodes, causing them to be passed to `createAnnotationArgumentFromValue` and producing errors.

### Changes Made

| File | Change |
|------|--------|
| `JavaClassOverAst.kt` | Added `ANNOTATION_METHOD` to method discovery. Added `ENUM_CONSTANT` to fields getter. |
| `JavaMemberOverAst.kt` | Implemented `annotationParameterDefaultValue` to find value after `DEFAULT_KEYWORD`. Fixed `JavaFieldOverAst` for enum constants (type, isStatic, isFinal). |
| `JavaAnnotationOverAst.kt` | Fixed `enumClassId` to use same package instead of java.lang fallback. Added `WHITE_SPACE` filtering in `createAnnotationArgument`. |
| `JavaTypeOverAst.kt` | Added `JavaClassifierTypeForEnumEntry` class for enum constant field types. |
| `FirJavaFacade.kt` | Fixed `enumEntriesOrigin` to use `FirDeclarationOrigin.Enhancement`. |

---

## Iteration 18: Nested Class Resolution

**Status**: ✅ Completed  
**Actual Impact**: +8 tests (74 → 66 failing)  
**Priority**: HIGH  
**Complexity**: MEDIUM

### What Was Done

Fixed resolution of nested class references like `Map.Entry`, `Outer.Inner`, etc. when the outer class is in binary (JDK or library classes).

### Root Cause

1. **`isResolved` was incorrect**: It returned `true` for any name containing a dot (e.g., `Map.Entry`), assuming dot-separated names were already fully qualified. But `Map.Entry` needs resolution - `Map` must be resolved to `java.util.Map` first.

2. **Initial fix used unreliable heuristic**: First attempted to distinguish packages from classes by case (lowercase = package, uppercase = class). This is fragile and doesn't handle unconventional naming.

### Correct Approach (following javac-wrapper pattern)

The proper solution is to **probe the symbol provider** with different package/class boundary splits until one resolves:

1. For `java.util.Map.Entry`, try:
   - `ClassId("java.util.Map", "Entry")` - not found
   - `ClassId("java.util", "Map.Entry")` - found! ✓

2. The resolution callback resolves the outer class first (`Map` → `java.util.Map`), then appends the nested class name.

### Changes Made

| File | Change |
|------|--------|
| `JavaTypeOverAst.kt` | Fixed `isResolved` to return `false` for names that aren't explicitly imported or locally resolved. Removed case-based heuristic. |
| `JavaResolutionContext.kt` | Added `resolveNestedClass()` that resolves outer class first (via same package, java.lang, star imports), then appends nested class name. |
| `JavaTypeConversion.kt` (FIR) | Replaced `createClassIdFromFqn` with `findClassId` that probes different package/class splits using the symbol provider until one resolves. |

### Key Code Change (FIR)

```kotlin
private fun findClassId(fqn: String, session: FirSession): ClassId? {
    val parts = fqn.split('.')
    
    // Try progressively longer class names (shorter package prefixes)
    for (classStartIndex in (parts.size - 1) downTo 0) {
        val packageFqName = if (classStartIndex == 0) FqName.ROOT
                           else FqName.fromSegments(parts.subList(0, classStartIndex))
        val relativeClassName = FqName.fromSegments(parts.subList(classStartIndex, parts.size))
        val classId = ClassId(packageFqName, relativeClassName, isLocal = false)
        
        if (session.symbolProvider.getClassLikeSymbolByClassId(classId) != null) {
            return classId
        }
    }
    return null
}
```

### Why This Is Better Than Heuristics

1. **No guessing**: Instead of assuming based on case, we verify with the symbol provider
2. **Handles edge cases**: Works for unconventional package/class names (e.g., `com.ACME.Foo`)
3. **Consistent with existing implementations**: Follows the same pattern as javac-wrapper's `ClassifierResolver`

### Tests Fixed

- `testMapEntry` — `Map.Entry` (JDK nested interface)
- `testSerializableInnerConstructorRef` — inner class constructor refs
- `testSerializableBoundInnerConstructorRef` — bound inner class refs
- +5 more nested class related tests

---

## Iteration 19: TYPE_USE Annotations on Type Arguments

**Status**: ✅ Completed  
**Actual Impact**: +5 tests (66 → 61 failing)  
**Priority**: MEDIUM  
**Complexity**: LOW (simpler than expected)

### What Was Done

Fixed parsing of TYPE_USE annotations on generic type arguments (e.g., `List<@NotNull Integer>`).

### Root Cause

The AST structure for annotated type arguments is:
```
TYPE: '@NotNull Integer'
  ANNOTATION: '@NotNull'
  WHITE_SPACE: ' '
  JAVA_CODE_REFERENCE: 'Integer'
```

In `createJavaType`, when creating a `JavaClassifierTypeOverAst`, we passed the `JAVA_CODE_REFERENCE` node (not the `TYPE` node). But the `ANNOTATION` is a sibling of `JAVA_CODE_REFERENCE`, directly under the `TYPE` node. The existing code only looked for annotations in `MODIFIER_LIST`, missing annotations directly on the TYPE node.

### Fix

In `createJavaType()`, when we have a `JAVA_CODE_REFERENCE` under a TYPE node, extract any `ANNOTATION` children from the TYPE node and pass them as `extraAnnotations`:

```kotlin
val referenceNode = typeNode.findChildByType("JAVA_CODE_REFERENCE")
if (referenceNode != null) {
    // TYPE_USE annotations on type arguments appear directly under the TYPE node
    val typeNodeAnnotations = typeNode.getChildrenByType("ANNOTATION")
        .map { JavaAnnotationOverAst(it, resolutionContext) }
    val allAnnotations = extraAnnotations + typeNodeAnnotations
    return JavaClassifierTypeOverAst(referenceNode, resolutionContext, allAnnotations)
}
```

### Changes Made

| File | Change |
|------|--------|
| `JavaTypeOverAst.kt` | Modified `createJavaType()` to extract `ANNOTATION` children from TYPE nodes and pass as `extraAnnotations` to `JavaClassifierTypeOverAst`. |

### Tests Added to JavaParsingTest.kt

- `testAnnotatedTypeArguments` — Single `@NotNull` on type argument
- `testAnnotatedTypeArgumentsMultiple` — `@NotNull` and `@Nullable` on Map key/value
- `testAnnotatedTypeArgumentInMethodReturn` — Annotation in method return type
- `testAnnotatedTypeArgumentInMethodParameter` — Annotation in method parameter type
- `testUnannotatedTypeArgument` — Verifies no spurious annotations

### Tests Fixed

- `testJavaCollectionOfNotNullToTypedArrayFailFast`
- `testJavaIteratorOfNotNullFailFast`
- `testJavaCollectionOfExplicitNotNullFailFast`
- +2 more nullability-related tests

---

## Iteration 20: Wildcard/Projection Edge Cases

**Status**: ✅ Completed (partial)  
**Actual Impact**: +7 tests (61 → 54 failing)  
**Priority**: MEDIUM  
**Complexity**: HIGH

### What Was Done

Fixed two separate issues with wildcard/generic type handling:

1. **Wildcard type arguments not parsed correctly** — TYPE nodes containing `?` were being incorrectly processed because the code looked for a nested TYPE before checking for QUEST.

2. **Inner class types missing implicit type arguments** — Non-static inner class types need implicit type arguments from their enclosing classes for FIR to process them correctly.

### Root Causes Found

**Issue 1: Wildcard Parsing**

The `createJavaType()` function had this logic:
```kotlin
val typeNode = node.findChildByType("TYPE") ?: node
if (typeNode.findChildByType("QUEST") != null) { ... }
```

For a TYPE node like `? extends A`:
```
TYPE: '? extends A'
  QUEST: '?'
  EXTENDS_KEYWORD: 'extends'
  TYPE: 'A'
```

The code would find the nested `TYPE: 'A'` first, assign it to `typeNode`, then check for QUEST on that node (not found). Fixed by checking for QUEST on the input TYPE node BEFORE looking for nested TYPE.

**Issue 2: Inner Class Type Arguments**

Java-direct's `typeArguments` only returned explicit type arguments from `REFERENCE_PARAMETER_LIST`. But for non-static inner classes, FIR expects implicit type arguments from outer classes.

For example, `Outer<T>.Inner<U>` should have `typeArguments = [T, U]`, not just `[U]`.

The javac-wrapper's `TreeBasedClassifierType.typeArguments` (lines 141-166) has this logic:
```kotlin
// For non-static inner classes, collect type parameters from outer classes
return arrayListOf<JavaClass>().apply {
    var outer = classifier.outerClass
    while (outer != null && !outer.isStatic) {
        add(outer)
        outer = outer.outerClass
    }
}.flatMap { it.typeParameters.map(::TreeBasedTypeParameterType) }
```

### Changes Made

| File | Change |
|------|--------|
| `JavaTypeOverAst.kt` | Fixed `createJavaType()` to check for QUEST on input TYPE node before looking for nested TYPE. |
| `JavaTypeOverAst.kt` | Added implicit outer class type arguments to `JavaClassifierTypeOverAst.typeArguments` for non-static inner classes. |
| `JavaTypeOverAst.kt` | Added `JavaTypeParameterTypeOverAst` class to represent type parameter references as implicit type arguments. |

### Tests Added to JavaParsingTest.kt

- `testCovariantWildcardReturnType` — Verifies wildcard type arguments in method return types
- `testUnboundedWildcard` — Tests unbounded `?`, `? extends Object`, and `? super String`

### Tests Fixed

- `testJavaGenericSynthProperty` — Star projection crash (inner class type args)
- `testDelegatedMembers` — Wildcard preserved (inner class type args)
- +5 more wildcard/generic related tests

### Remaining Issue

`testInheritanceWithWildcard` still fails with `NoSuchMethodError: 'Y D.foo()'`. This appears to be a FIR-level issue with fake override generation for covariant wildcard returns across Java inheritance hierarchies. The java-direct parsing is correct (classifiers resolve properly), but FIR doesn't generate the expected bridge method.

---

## Iteration 22: FIR-Level TYPE_USE Annotation Filtering

**Status**: ✅ Completed  
**Actual Impact**: +1 box test, +3 phased tests (37→36 box, 31→28 phased)  
**Priority**: HIGH  
**Complexity**: MEDIUM

### What Was Done

Implemented FIR-level filtering of type annotations to only include annotations that have `@Target(TYPE_USE)`. This matches javac-wrapper's behavior where annotations are only applied to types if they explicitly declare TYPE_USE as a target.

### Changes Made

| File | Change |
|------|--------|
| `JavaTypeConversion.kt` | Added `isTypeUseAnnotation()` function that resolves annotation class and checks for `@Target(TYPE_USE)` |
| `JavaTypeConversion.kt` | Added `hasTypeUseTarget()` and `isTypeUseElement()` helper functions |
| `JavaTypeConversion.kt` | Modified `toConeTypeProjection()` to filter annotations before converting to FIR |

### Key Implementation

```kotlin
private fun JavaAnnotation.isTypeUseAnnotation(session: FirSession): Boolean {
    val annotationClassId = classId ?: return false
    val annotationClass = session.symbolProvider.getClassLikeSymbolByClassId(annotationClassId)?.fir
        as? FirRegularClass ?: return false
    
    val targetAnnotation = annotationClass.annotations.find { firAnnotation ->
        val targetClassId = firAnnotation.annotationTypeRef.coneType.classId
        targetClassId == JvmStandardClassIds.Annotations.Java.Target ||
                targetClassId == StandardClassIds.Annotations.Target
    } ?: return false
    
    return hasTypeUseTarget(targetAnnotation)
}
```

### Tests Fixed

- `testSyntheticSmartCast` - No longer has spurious `@Nullable` on types
- `testBasicWithAnnotatedJava` - TYPE_USE annotations filtered correctly
- +2 more phased tests

### Why Fewer Tests Fixed Than Expected

The original estimate of ~15 tests was based on all annotation-related diffs. However, many remaining failures have other root causes:
1. Some annotations ARE TYPE_USE and should appear (correct behavior)
2. Some tests have unrelated issues (nested class resolution, etc.)
3. Some tests involve annotation argument parsing, not type annotations

### Previous Analysis (Archived)

**Root Cause Analysis**:

**javac-wrapper** (`filterTypeAnnotations()` in `utils.kt`):
- Resolves annotation class via `annotation.resolve()`
- Checks if `@Target` annotation contains `TYPE_USE`
- If resolution fails or no `@Target` found → annotation is **skipped**

**java-direct** (before fix):
- Cannot resolve annotations (`resolve()` returns null)
- Uses blocklist approach instead
- If annotation not in blocklist → annotation **passes through**

**Why syntactic approach won't work**: PSI/KMP parser places ALL annotations in `MODIFIER_LIST` regardless of their position relative to other modifiers. There's no equivalent to javac's `JCAnnotatedType` to distinguish type annotations from declaration annotations syntactically.

---

## Iteration 23: Sibling Nested Class Resolution in Supertypes

**Status**: 🔲 Planned  
**Expected Impact**: ~5 tests  
**Priority**: MEDIUM  
**Complexity**: MEDIUM

### Problem Statement

Nested classes within the same outer class aren't being resolved when used as supertypes.

### Symptoms

```
MISSING_DEPENDENCY_SUPERCLASS: Cannot access 'Base' which is a supertype of 'Test.Derived'
MISSING_DEPENDENCY_SUPERCLASS: Cannot access 'KotlinInner' which is a supertype of 'K2.K3'
MISSING_DEPENDENCY_CLASS: Cannot access class 'A'
```

### Affected Tests

1. **testOffOrderMultiBoundGenericOverride**
   ```java
   public class Test {
       public static class Base { ... }
       public static class Derived extends Base { ... }  // Base not resolved
   }
   ```

2. **testOuterInnerClasses**
   ```java
   public class J1 extends KotlinOuter {
       public class J2 extends KotlinInner { ... }  // KotlinInner not resolved
   }
   ```

3. **testHugeMixedCapturedType**
   ```java
   public class JavaClass {
       public interface A<...> { ... }
       public interface B<...> { ... }
       // Methods using A, B - nested interfaces not resolved
   }
   ```

4. **testSmartcastToStarProjectedSubclass**
   ```java
   public interface Option<T> {
       public final class Some<T> implements Option<T> { ... }
   }
   ```

### Root Cause

When resolving supertype `Base` in `class Derived extends Base`:
1. Current resolution searches: same package, java.lang, star imports
2. **Missing**: sibling nested classes within the same outer class

In `Test.Derived extends Base`:
- `Base` is actually `Test.Base` (sibling nested class)
- Resolution doesn't try `OuterClass.SimpleName` for siblings

### Phase 1: Analysis

1. Debug resolution context:
   ```kotlin
   // In JavaResolutionContext.resolveWithCallback
   println("DEBUG: resolving '$name' in context of ${containingClassFqName}")
   ```

2. Trace supertype resolution path:
   ```kotlin
   // In JavaClassOverAst.supertypes
   println("DEBUG: supertype name='${typeNode.text}', resolved=${resolvedFqn}")
   ```

### Phase 2: Implementation

**File to modify**: `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaResolutionContext.kt`

1. **Add sibling nested class resolution in `resolveSimpleName()`**:
   ```kotlin
   private fun resolveSimpleName(name: String, tryResolve: (String) -> Boolean): String? {
       // ... existing steps (explicit imports, same package, java.lang, star imports)
       
       // Step N: Sibling nested classes (same outer class)
       val containingClass = containingClassProvider?.invoke()
       if (containingClass != null) {
           val outerClass = containingClass.outerClass
           if (outerClass != null) {
               val outerFqn = outerClass.fqName?.asString()
               if (outerFqn != null) {
                   val siblingCandidate = "$outerFqn.$name"
                   if (tryResolve(siblingCandidate)) {
                       return siblingCandidate
                   }
               }
           }
       }
       
       // ... rest of resolution
   }
   ```

2. **Ensure containing class is available during supertype resolution**:
   - Check that `JavaResolutionContext` receives the containing class reference
   - May need to pass it through supertype resolution chain

### Phase 3: Validation

1. Run affected tests:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "*testOffOrderMultiBoundGenericOverride*" -q
   ./gradlew :compiler:java-direct:test --tests "*testOuterInnerClasses*" -q
   ./gradlew :compiler:java-direct:test --tests "*testSmartcastToStarProjectedSubclass*" -q
   ```

2. Run full test suite to check for regressions

### Success Criteria

- Sibling nested classes resolve correctly in supertypes
- ~5 test improvements expected
- No regressions

---

## Iteration 24: Cyclic Type Parameter Bounds Detection

**Status**: 🔲 Planned  
**Expected Impact**: 1 test  
**Priority**: LOW  
**Complexity**: HIGH

### Problem Statement

Infinite recursion when resolving mutually recursive type parameter bounds causes StackOverflowError.

### Symptoms

```
java.lang.StackOverflowError
    at JavaClassifierTypeOverAst.getClassifierQualifiedName(JavaTypeOverAst.kt:121)
    at JavaTypeConversionKt.toConeKotlinTypeForFlexibleBound
    ...
    at FirSignatureEnhancement.enhanceTypeParameterBounds
```

### Affected Test

**testSignatureEnhancementCycleTypeBound**
```java
public class JavaA<T extends JavaB> { }
public class JavaB<T extends JavaA> { }
```

### Root Cause

When FIR enhances type parameter bounds:
1. `JavaA<T extends JavaB>` → resolve `JavaB`
2. `JavaB<T extends JavaA>` → resolve `JavaA`
3. `JavaA<T extends JavaB>` → resolve `JavaB` (infinite loop!)

The `classifierQualifiedName` getter triggers type resolution, which triggers signature enhancement, which reads type parameter bounds, calling `classifierQualifiedName` again.

### Phase 1: Analysis

1. Add stack trace logging to identify the recursion entry point:
   ```kotlin
   // In JavaClassifierTypeOverAst.classifierQualifiedName
   if (Thread.currentThread().stackTrace.count { it.methodName == "getClassifierQualifiedName" } > 5) {
       throw IllegalStateException("DEBUG: Potential cycle detected\n" + 
           Thread.currentThread().stackTrace.take(50).joinToString("\n"))
   }
   ```

2. Compare with javac-wrapper's behavior - how does it avoid this cycle?

### Phase 2: Implementation Options

**Option A: Thread-local cycle detection**

**File to modify**: `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt`

```kotlin
class JavaClassifierTypeOverAst(...) {
    companion object {
        private val resolvingTypes = ThreadLocal<MutableSet<String>>()
    }
    
    override val classifierQualifiedName: String
        get() {
            val resolving = resolvingTypes.get() ?: mutableSetOf<String>().also { resolvingTypes.set(it) }
            val key = "${System.identityHashCode(this)}"
            
            if (key in resolving) {
                // Cycle detected - return raw type name without resolution
                return rawTypeName
            }
            
            resolving.add(key)
            try {
                // existing resolution logic
            } finally {
                resolving.remove(key)
            }
        }
}
```

**Option B: Lazy initialization with sentinel value**

```kotlin
private var cachedQualifiedName: String? = null
private var isResolving = false

override val classifierQualifiedName: String
    get() {
        cachedQualifiedName?.let { return it }
        
        if (isResolving) {
            // Cycle - return unresolved name
            return rawTypeName
        }
        
        isResolving = true
        try {
            val resolved = /* resolution logic */
            cachedQualifiedName = resolved
            return resolved
        } finally {
            isResolving = false
        }
    }
```

### Phase 3: Validation

1. Run the failing test:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "*testSignatureEnhancementCycleTypeBound*" -q
   ```

2. Verify no infinite loops or StackOverflowErrors

3. Run full test suite - cycle detection should be invisible to non-cyclic cases

### Success Criteria

- No StackOverflowError on cyclic type bounds
- Cyclic references resolve to raw/unqualified names (breaking the cycle)
- 1 test improvement expected
- No regressions

---

## Remaining Work

### Current Status After Iteration 20: 1112/1166 box tests (95.4%)

| Category | Count | Example Tests |
|----------|-------|---------------|
| FIR-level Issues | ~1 | `testInheritanceWithWildcard` (covariant override bridge) |
| Visibility Issues | ~2 | `testContractAndRawField` |
| Annotation Edge Cases | ~10 | `testJavaAnnotation`, `testConstValAsAnnotationArgumentInJava` |
| Enum Handling | ~6 | `testJavaEnumValues`, `testEnumEntriesFromJava` |
| Reflection/Metadata | ~5 | `testCallableReferenceToJavaField` |
| Other Edge Cases | ~30 | Various |

### Completed Iterations Summary

| Iteration | Target | Tests Fixed |
|-----------|--------|-------------|
| 18 | Nested Class Resolution | ✅ +8 |
| 19 | TYPE_USE on Type Args | ✅ +5 |
| 20 | Wildcard Edge Cases | ✅ +7 |
| 21 | Visibility Issues | ~3 |

### Key Insight: Remaining Failures

54 tests still failing. Categories include:
- FIR-level issues (fake override generation, capture conversion edge cases)
- Annotation edge cases (const val references, annotation instantiation)
- Visibility/access issues
- Baseline content diffs

### Potential Future Iterations

**Iteration 22: Const Val Annotation Arguments**
- Handle `REFERENCE_EXPRESSION` for const val references
- Distinguish from enum constant references
- May need FIR-level resolution

**Iteration 23: testInheritanceWithWildcard Investigation**
- Debug FIR fake override generation for covariant wildcard returns
- May require FIR team consultation

**Iteration 24: Baseline Diff Triage**
- Review each baseline diff individually
- Categorize: bug, acceptable, or needs baseline update

---

## Document Change Log

- 2026-03-10: **Iteration 20** - Fixed wildcard parsing and inner class type arguments (+7 tests)
- 2026-03-06: **Major cleanup** - Archived iterations 7-16, condensed document
- 2026-03-05: Iterations 11-16 completed (ad-hoc approach)
- 2026-03-04: Iterations 7-10 completed
- 2026-02-23: Initial structure with iterations 1-6
