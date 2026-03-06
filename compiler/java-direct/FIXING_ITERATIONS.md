# Java-Direct: Fixing Iterations

## Document Purpose

This document contains iteration plans for the `java-direct` module.

**Prerequisites**: Read `AGENT_INSTRUCTIONS.md` before starting any iteration  
**Status**: Iteration 16 complete — 1317/1493 tests passing (88.2%)  
**Last Updated**: 2026-03-06

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

**Status**: 🔲 Planned  
**Expected Impact**: ~20 tests  
**Priority**: HIGH  
**Complexity**: LOW-MEDIUM

### Problem Statement

Annotation method default values are not being parsed. When a Java annotation declares a method with a `default` value, the java-direct module returns `null` instead of the default value.

```java
public @interface MyAnnotation {
    String value() default "default";  // ← default value not parsed
    int count() default 42;
}
```

This causes Kotlin code using these annotations to fail with `NAMED_PARAMETER_NOT_FOUND` or `TOO_MANY_ARGUMENTS` when:
- Omitting parameters that have defaults
- Using named parameters syntax

### Root Cause

In `JavaMethodOverAst` (`compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt`), the annotation default value properties are hardcoded:

```kotlin
override val annotationParameterDefaultValue: JavaAnnotationArgument? get() = null
override val hasAnnotationParameterDefaultValue: Boolean get() = false
```

### Reference Implementation

PSI implementation in `JavaMethodImpl.java` (`compiler/frontend.common.jvm/src/org/jetbrains/kotlin/load/java/structure/impl/JavaMethodImpl.java`):

```java
@Override
@Nullable
public JavaAnnotationArgument getAnnotationParameterDefaultValue() {
    PsiMethod psiMethod = getPsi();
    if (psiMethod instanceof PsiAnnotationMethod) {
        PsiAnnotationMemberValue defaultValue = ((PsiAnnotationMethod) psiMethod).getDefaultValue();
        if (defaultValue != null) {
            return JavaAnnotationArgumentImpl.Factory.create(defaultValue, null, getSourceFactory());
        }
    }
    return null;
}
```

### Affected Tests (20+ tests)

**NAMED_PARAMETER_NOT_FOUND (11 tests)**:
- `testAnnotationsOnNonExistentAccessors`
- `testCallableReferencesOnEnumMembers`
- `testEnumCtorAnnotation`
- `testEnumInteractionWithJava`
- `testJavaToKotlinAnnotationConflictingTargets`
- `testKotlinAnnotations`
- `testSimpleEnumWithSingleArg`
- `testPropertyAnnotations`
- `testAnnotatedEnumEntry`
- `testEnumArgWithDefault`
- `testJavaFieldAccessedAsProperty`

**TOO_MANY_ARGUMENTS (9 tests)**:
- `testEnumEntryWithAnnotation`
- `testEnumWithCompanion`
- `testEnumWithDefaultParameter`
- `testAnnotationUseSiteTarget`
- `testEnumConstructorInnerClass`
- `testJavaAnnotationOnKotlinClass`
- `testEnumEntryCtorCall`
- `testEnumEntryWithLambda`
- `testAnnotationDefaultArgs`

### Symptoms

```
NAMED_PARAMETER_NOT_FOUND: Cannot find a parameter with this name: value
```

```
TOO_MANY_ARGUMENTS: Too many arguments for @MyAnnotation()
```

When FIR processes annotation usage, it expects to find default values from `javaMethod.annotationParameterDefaultValue` (used in `FirJavaFacade.kt:384`):

```kotlin
javaMethod.annotationParameterDefaultValue?.let { javaDefaultValue ->
    firValueParameter.lazyDefaultValue = lazy {
        javaDefaultValue.toFirExpression(...)
    }
}
```

### Phase 1: Analysis

**Goal**: Understand AST structure for annotation method default values

1. Find test data with annotation defaults:
   ```bash
   grep -r "default" compiler/testData/codegen/box/annotations/ | grep "\.java:" | head -5
   ```

2. Create minimal test case and dump AST:
   ```java
   public @interface TestAnnotation {
       String value() default "hello";
   }
   ```

3. Add debug in `JavaMethodOverAst`:
   ```kotlin
   override val annotationParameterDefaultValue: JavaAnnotationArgument?
       get() {
           // Look for DEFAULT node or "default" keyword
           val methodNode = node // assuming node is the method declaration
           throw IllegalStateException("DEBUG: method AST dump:\n${methodNode.dump()}")
       }
   ```

4. Expected AST structure (to verify):
   ```
   ANNOTATION_METHOD
   ├── TYPE
   │   └── JAVA_CODE_REFERENCE (String)
   ├── IDENTIFIER (value)
   ├── PARAMETER_LIST
   ├── DEFAULT_KEYWORD (default)
   └── LITERAL_EXPRESSION ("hello")
   ```

### Phase 2: Implementation

**File to modify**: `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt`

1. **Check if method is in an annotation interface**:
   ```kotlin
   private fun isAnnotationMethod(): Boolean {
       // The containing class should be an annotation (@interface)
       val containingClass = containingClass
       return containingClass.isAnnotationType
   }
   ```

2. **Parse the default value**:
   ```kotlin
   override val annotationParameterDefaultValue: JavaAnnotationArgument?
       get() {
           if (!isAnnotationMethod()) return null
           
           // Look for default value in AST
           // Structure may be: METHOD -> ... -> DEFAULT_KEYWORD -> VALUE_EXPRESSION
           // Or: ANNOTATION_METHOD -> DEFAULT_VALUE -> EXPRESSION
           
           val defaultKeyword = node.findChildByType("DEFAULT_KEYWORD") ?: return null
           
           // The value expression should follow the default keyword
           val valueNode = defaultKeyword.nextSibling() 
               ?: node.children.dropWhile { it != defaultKeyword }.drop(1).firstOrNull()
               ?: return null
           
           // Reuse the annotation argument creation logic from Iteration 17
           return createAnnotationArgument(valueNode, resolutionContext)
       }
   
   override val hasAnnotationParameterDefaultValue: Boolean
       get() = annotationParameterDefaultValue != null
   ```

3. **Alternative if AST structure differs**:
   ```kotlin
   override val annotationParameterDefaultValue: JavaAnnotationArgument?
       get() {
           if (!isAnnotationMethod()) return null
           
           // Try finding a DEFAULT_VALUE child node
           val defaultValue = node.findChildByType("DEFAULT_VALUE")
           if (defaultValue != null) {
               val valueExpr = defaultValue.children.firstOrNull() ?: return null
               return createAnnotationArgument(valueExpr, resolutionContext)
           }
           
           // Try finding default keyword followed by value
           val text = node.text
           val defaultIdx = text.indexOf(" default ")
           if (defaultIdx >= 0) {
               // Parse value after "default "
               // This is a fallback - prefer structured AST parsing
           }
           
           return null
       }
   ```

4. **Ensure `createAnnotationArgument` is accessible** from `JavaMethodOverAst`:
   - Either move it to a shared utility class
   - Or make it a top-level function in the same package
   - It was implemented in `JavaAnnotationOverAst.kt` during Iteration 17

### Phase 3: Validation

1. Run a single affected test:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated\$Annotations\$*testKotlinAnnotations*" -q
   ```

2. Run all annotation tests:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated\$Annotations.*" -q
   ```

3. Run full suite:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated" -q 2>&1 | tail -5
   ```

### Success Criteria

- `NAMED_PARAMETER_NOT_FOUND` errors for annotation defaults are resolved
- `TOO_MANY_ARGUMENTS` errors for annotation defaults are resolved
- ~20 test improvements expected
- No regressions in existing annotation tests

### Implementation Notes

1. **Sharing code with Iteration 17**: The `createAnnotationArgument()` function created in Iteration 17 should be reusable here. Consider refactoring it to a shared location.

2. **Null handling**: Some annotation methods don't have defaults. Make sure to return `null` correctly in those cases.

3. **Type consistency**: The default value should be convertible to the method's declared return type. FIR handles this validation, but ensure the value types are correct.

---

## Iteration 18: Nested Class Resolution

**Status**: 🔲 Planned  
**Expected Impact**: ~10 tests  
**Priority**: HIGH  
**Complexity**: MEDIUM

### Problem Statement

When resolving type references like `Outer.Inner`, `Map.Entry`, or `KotlinTypeChecker.TypeConstructorEquality`, the java-direct module fails to resolve nested classes when the outer class is in binary (JDK or library classes).

### Symptoms

```
MISSING_DEPENDENCY_CLASS: Cannot access class 'Outer.Inner'. Check your module classpath.
MISSING_DEPENDENCY_SUPERCLASS: Cannot access 'Map.Entry' which is a supertype.
MISSING_DEPENDENCY_CLASS: Cannot access class 'KotlinTypeChecker.TypeConstructorEquality'.
```

### Affected Tests

- `testSerializableBoundInnerConstructorRef` — `Outer.Inner`
- `testSerializableInnerConstructorRef` — `Outer.Inner`
- `testMapEntry` — `Map.Entry` (JDK)
- `testSamWithEquals` — `KotlinTypeChecker.TypeConstructorEquality`
- `testHugeMixedCapturedType` — nested class 'A'
- `testOuterInnerClasses` — `KotlinInner` superclass

### Phase 1: Analysis

**Goal**: Understand how nested class names appear in AST and how resolution currently works

1. Debug with a failing test:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated\$BuiltinStubMethods\$ExtendJavaClasses.testMapEntry" -q
   ```

2. Add exception-based debugging in `JavaResolutionContext.resolveWithCallback`:
   ```kotlin
   fun resolveWithCallback(simpleName: String, tryResolve: (String) -> Boolean): String? {
       if (simpleName.contains(".")) {
           throw IllegalStateException("DEBUG: nested class reference: $simpleName")
       }
       // existing code...
   }
   ```

3. Trace how `Map.Entry` flows through the resolution:
   - Is it coming as `Map.Entry` or separately?
   - At what point does resolution fail?

### Phase 2: Understanding the Resolution Flow

The current resolution in `JavaResolutionContext.resolveWithCallback`:
1. Try same package: `packageFqName.child(simpleName)`
2. Try `java.lang` package
3. Try star imports

**Problem**: This assumes `simpleName` is a simple name (no dots). When we have `Map.Entry`:
- We need to resolve `Map` first to get `java.util.Map`
- Then look for nested class `Entry` inside `Map`

### Phase 3: Implementation

**Files to modify**: 
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaResolutionContext.kt`
- `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt`

1. **Handle dot-separated names in `classifierQualifiedName`**:
   ```kotlin
   // In JavaClassifierTypeOverAst.classifierQualifiedName
   override val classifierQualifiedName: String?
       get() {
           val reference = typeReference ?: return null
           
           // Check if it's already a qualified name (contains dots)
           if (reference.contains('.')) {
               // Could be:
               // 1. Fully qualified: java.util.Map.Entry
               // 2. Outer.Inner where Outer needs resolution
               
               // First try as fully qualified
               // If that fails, try splitting and resolving outer class
               return reference
           }
           
           // Simple name resolution (existing logic)
           // ...
       }
   ```

2. **Add nested class resolution to callback pattern**:
   ```kotlin
   // In JavaResolutionContext
   fun resolveWithCallback(name: String, tryResolve: (String) -> Boolean): String? {
       // Handle nested class references (e.g., "Outer.Inner")
       if (name.contains('.')) {
           return resolveNestedClass(name, tryResolve)
       }
       
       // Existing simple name resolution...
   }
   
   private fun resolveNestedClass(name: String, tryResolve: (String) -> Boolean): String? {
       // Try as fully qualified first
       if (tryResolve(name)) return name
       
       // Split into potential outer class and nested class
       val dotIndex = name.indexOf('.')
       val outerName = name.substring(0, dotIndex)
       val nestedPart = name.substring(dotIndex + 1)
       
       // Resolve outer class
       val resolvedOuter = resolveWithCallback(outerName, tryResolve) ?: return null
       
       // Try outer + nested (using $ for JVM nested class convention)
       val nestedFqName = "$resolvedOuter.$nestedPart"
       if (tryResolve(nestedFqName)) return nestedFqName
       
       // Also try with $ separator (JVM internal name)
       val nestedJvmName = "$resolvedOuter\$$nestedPart"
       if (tryResolve(nestedJvmName)) return nestedJvmName
       
       return null
   }
   ```

3. **Handle deeply nested classes** (e.g., `A.B.C`):
   ```kotlin
   private fun resolveNestedClass(name: String, tryResolve: (String) -> Boolean): String? {
       // Try as fully qualified first
       if (tryResolve(name)) return name
       
       // Progressive resolution: try resolving more of the prefix
       val parts = name.split('.')
       for (i in 1 until parts.size) {
           val outerParts = parts.subList(0, i)
           val nestedParts = parts.subList(i, parts.size)
           
           val outerName = outerParts.joinToString(".")
           val resolvedOuter = if (outerName.contains('.')) {
               resolveNestedClass(outerName, tryResolve)
           } else {
               resolveSimpleName(outerName, tryResolve)
           }
           
           if (resolvedOuter != null) {
               val fullNested = "$resolvedOuter.${nestedParts.joinToString(".")}"
               if (tryResolve(fullNested)) return fullNested
           }
       }
       return null
   }
   ```

### Phase 4: Validation

1. Test `Map.Entry` resolution:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated\$BuiltinStubMethods\$ExtendJavaClasses.testMapEntry" -q
   ```

2. Test inner class constructor refs:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated\$Invokedynamic\$Serializable.testSerializableInnerConstructorRef" -q
   ```

3. Test SAM with nested interface:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstPhasedTestGenerated\$Resolve\$SamConversions.testSamWithEquals" -q
   ```

### Success Criteria

- `testMapEntry` passes
- `testSerializableInnerConstructorRef` passes
- `testSamWithEquals` passes
- ~8-10 test improvements expected

---

## Iteration 19: TYPE_USE Annotations on Type Arguments

**Status**: 🔲 Planned  
**Expected Impact**: ~5 tests  
**Priority**: MEDIUM  
**Complexity**: HIGH

### Problem Statement

TYPE_USE annotations on generic type arguments (e.g., `List<@NotNull Integer>`) are not being parsed and attached to the type arguments.

### Symptoms

```
expected: <OK> but was: <Fail: should throw on get()>
```

The test expects a `NullPointerException` due to `@NotNull` enforcement, but it doesn't throw because the annotation isn't recognized.

### Affected Tests

- `testJavaCollectionOfNotNullToTypedArrayFailFast`
- `testJavaIteratorOfNotNullFailFast`
- `testJavaCollectionOfExplicitNotNullFailFast`

### Test Pattern

```java
// FILE: J.java
public class J {
    public static List<@NotNull Integer> listOfNotNull() {
        return Collections.singletonList(null);
    }
}
```

### Phase 1: Analysis

**Goal**: Understand AST structure for annotated type arguments

1. Debug the test:
   ```bash
   ./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated\$Ranges\$JavaInterop.testJavaCollectionOfNotNullToTypedArrayFailFast" -q
   ```

2. Add AST dump in type argument parsing:
   ```kotlin
   // In JavaClassifierTypeOverAst.typeArguments
   override val typeArguments: List<JavaType?>
       get() {
           val typeArgList = node.findChildByType("TYPE_ARGUMENT_LIST") ?: return emptyList()
           throw IllegalStateException("DEBUG: typeArgList.dump=\n${typeArgList.dump()}")
       }
   ```

3. Look for annotation nodes within type arguments

### Phase 2: Implementation

**File to modify**: `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaTypeOverAst.kt`

1. **Parse annotations on type arguments**:
   ```kotlin
   override val typeArguments: List<JavaType?>
       get() {
           val typeArgList = node.findChildByType("TYPE_ARGUMENT_LIST") ?: return emptyList()
           return typeArgList.getChildrenByType("TYPE").map { typeNode ->
               // Check for annotations on this type argument
               val annotations = typeNode.getChildrenByType("ANNOTATION").map { annoNode ->
                   JavaAnnotationOverAst(annoNode, resolutionContext)
               }
               createTypeWithAnnotations(typeNode, annotations)
           }
       }
   
   private fun createTypeWithAnnotations(
       typeNode: JavaSyntaxNode, 
       annotations: List<JavaAnnotation>
   ): JavaType {
       val baseType = JavaTypeOverAst.create(typeNode, resolutionContext)
       // Attach annotations to the type
       return if (annotations.isEmpty()) baseType
              else JavaAnnotatedTypeOverAst(baseType, annotations)
   }
   ```

2. **Create wrapper for annotated types** (if needed):
   ```kotlin
   class JavaAnnotatedTypeOverAst(
       private val delegate: JavaType,
       override val annotations: List<JavaAnnotation>
   ) : JavaType by delegate, JavaAnnotatedType {
       // Delegate all JavaType methods but override annotations
   }
   ```

3. **Alternative approach**: Modify existing type classes to accept annotations in constructor:
   ```kotlin
   class JavaClassifierTypeOverAst(
       node: JavaSyntaxNode,
       private val resolutionContext: JavaResolutionContext,
       private val additionalAnnotations: List<JavaAnnotation> = emptyList()
   ) : JavaTypeOverAst(node), JavaClassifierType {
       override val annotations: List<JavaAnnotation>
           get() = additionalAnnotations + parseAnnotationsFromNode()
   }
   ```

### Phase 3: Validation

```bash
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated\$Ranges\$JavaInterop.testJavaCollectionOfNotNullToTypedArrayFailFast" -q
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated\$Ranges\$JavaInterop.testJavaIteratorOfNotNullFailFast" -q
```

### Success Criteria

- Nullability checks trigger as expected
- ~3-5 test improvements expected

---

## Iteration 20: Wildcard/Projection Edge Cases

**Status**: 🔲 Planned  
**Expected Impact**: ~5 tests  
**Priority**: MEDIUM  
**Complexity**: HIGH

### Problem Statement

Complex wildcard scenarios fail due to:
1. Incorrect fake override generation with covariant wildcard returns
2. Star projections not properly handled after capture conversion
3. Wildcards not preserved in reflection/delegation scenarios

### Symptoms

```
java.lang.NoSuchMethodError: 'Y D.foo()'
```

```
java.lang.IllegalArgumentException: There should left no projections after capture conversion
```

```
Expected <[..., ? extends java.util.Set<...>]>, actual <[..., java.util.Set<...>]>
```

### Affected Tests

- `testInheritanceWithWildcard` — NoSuchMethodError
- `testJavaGenericSynthProperty` — Star projection crash
- `testDelegatedMembers` — Wildcard not preserved

### Phase 1: Analysis — testInheritanceWithWildcard

**Test structure**:
```java
interface A { X<? extends A> foo(); }
interface B extends A { 
    @Override Y<? extends B> foo();  // Covariant override
}
class BImpl implements B { ... }
```

```kotlin
private class D : A, BImpl()  // Multiple inheritance
fun box() = D().foo()  // NoSuchMethodError
```

**Debug approach**:
1. Examine how the method signature is being constructed
2. Check if fake override is generated correctly
3. May require FIR-level investigation

```kotlin
// Add debug in method resolution
throw IllegalStateException("DEBUG: method=${method.name}, returnType=${method.returnType}")
```

### Phase 2: Analysis — testJavaGenericSynthProperty

**Test structure**:
```java
public class JOuter<O1, O2> {
    public JInner<Box<O1>, ?> instance(O1 s1, O2 s2) { ... }
}
```

The `?` wildcard in `JInner<Box<O1>, ?>` is causing capture conversion issues.

**Debug approach**:
1. Check how wildcards are represented in `JavaWildcardTypeOverAst`
2. Verify FIR type conversion handles nested wildcards correctly

### Phase 3: Implementation

This iteration may require changes in multiple places:

1. **Review `JavaWildcardTypeOverAst`** construction:
   ```kotlin
   class JavaWildcardTypeOverAst(
       node: JavaSyntaxNode,
       private val resolutionContext: JavaResolutionContext
   ) : JavaTypeOverAst(node), JavaWildcardType {
       override val bound: JavaType?
           get() = // Ensure bound is correctly parsed
       
       override val isExtends: Boolean
           get() = // Check for "extends" keyword or default (unbounded = extends Object)
   }
   ```

2. **Check FIR conversion** in `JavaTypeConversion.kt`:
   - How are wildcards converted to `ConeKotlinTypeProjection`?
   - Is `ConeStarProjection` used correctly for `?`?

3. **Review delegation handling** in FIR:
   - Wildcards should be preserved in delegated member signatures
   - May need special handling for reflection metadata

### Phase 4: Validation

```bash
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated\$JavaInterop.testInheritanceWithWildcard" -q
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated\$Properties.testJavaGenericSynthProperty" -q
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated\$GenericJvmSignature.testDelegatedMembers" -q
```

### Success Criteria

- All 3 wildcard tests pass
- No regressions in other generic tests

### Notes

This iteration may reveal FIR-level issues rather than java-direct issues. If so, document findings and potentially file issues for FIR team.

---

## Iteration 21: Raw Type Visibility

**Status**: 🔲 Planned  
**Expected Impact**: ~3 tests  
**Priority**: LOW  
**Complexity**: LOW

### Problem Statement

Protected field access through raw type inheritance incorrectly reports visibility errors.

### Symptoms

```
INVISIBLE_REFERENCE: Cannot access 'field instance': it is protected in 'Derived'.
```

### Affected Tests

- `testContractAndRawField`
- `testJavaAnnotationConstructorTypes`
- `testWeirdCharBuffers`

### Test Pattern

```java
public abstract class Base<T> {
    protected T instance;  // Protected field in generic class
}
public class Derived extends Base<String> {
    // Inherits protected field
}
```

```kotlin
fun Base<Any>.confirmOrFail(): String {
    require(this is Derived)
    return instance  // Should be accessible, but reports INVISIBLE
}
```

### Phase 1: Analysis

1. Debug visibility calculation:
   ```kotlin
   // In JavaFieldOverAst.visibility or related
   throw IllegalStateException("DEBUG: field=${name}, visibility=${visibility}, containingClass=${containingClass}")
   ```

2. Check if the issue is:
   - Wrong visibility being parsed
   - Raw type affecting visibility lookup
   - Smart cast not considered for protected access

### Phase 2: Implementation

**File to modify**: `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaMemberOverAst.kt`

1. **Verify visibility parsing**:
   ```kotlin
   class JavaFieldOverAst(...) : JavaField {
       override val visibility: Visibility
           get() {
               // Check modifiers
               val modifiers = node.findChildByType("MODIFIER_LIST")
               return when {
                   modifiers?.hasChildWithType("PROTECTED") == true -> Visibilities.Protected
                   modifiers?.hasChildWithType("PRIVATE") == true -> Visibilities.Private
                   modifiers?.hasChildWithType("PUBLIC") == true -> Visibilities.Public
                   else -> JavaVisibilities.PackageVisibility
               }
           }
   }
   ```

2. **Check inheritance chain handling**:
   - Protected members should be accessible in subclasses
   - Raw type erasure shouldn't affect this

### Phase 3: Validation

```bash
./gradlew :compiler:java-direct:test --tests "JavaUsingAstBoxTestGenerated\$Contracts.testContractAndRawField" -q
```

### Success Criteria

- Protected field access works correctly
- ~3 test improvements expected

---

## Remaining Work

### Current Status After Iteration 17: 1321/1493 (88.5%)

| Category | Count | Notes |
|----------|-------|-------|
| Baseline/Content Diffs | 104 | Need individual triage |
| Nested Class Resolution | 12 | `Outer.Inner` in binary classes |
| Missing Dep Superclass | 8 | Supertype resolution |
| Visibility Issues | 6 | Protected/package-private |
| Nullability TYPE_USE | 5 | `List<@NotNull T>` |
| Abstract Member | 3 | Override detection |
| Const Val in Annotations | 2 | Reference expression resolution |
| Annotation Method Access | 2 | Annotation instantiation feature |
| Other Edge Cases | 30 | Various |

### Realistic Expectations for Iterations 18-21

| Iteration | Target | Expected Tests Fixed |
|-----------|--------|---------------------|
| 18 | Nested Class Resolution | ~10-12 |
| 19 | TYPE_USE on Type Args | ~5 |
| 20 | Wildcard Edge Cases | ~3-5 |
| 21 | Visibility Issues | ~6 |

**Projected Status**: ~1350/1493 (~90.4%)

### Key Insight: Baseline Diffs Dominate

104 of 172 failures (60%) are baseline/content diffs. These require individual investigation to determine:
- Is the output incorrect? → Fix the code
- Is the output correct but different? → Update the baseline
- Is it an acceptable variation? → Mark as expected

### Potential Future Iterations

**Iteration 22: Const Val Annotation Arguments**
- Handle `REFERENCE_EXPRESSION` for const val references
- Distinguish from enum constant references
- May need FIR-level resolution

**Iteration 23: Baseline Diff Triage**
- Review each baseline diff individually
- Categorize: bug, acceptable, or needs baseline update

**Iteration 24: Missing Dep Superclass**
- Investigate supertype resolution failures
- May overlap with nested class resolution

**Iteration 25+: Modern Java Features** (if needed)
- Records, sealed classes, pattern matching

---

## Document Change Log

- 2026-03-06: **Major cleanup** - Archived iterations 7-16, condensed document
- 2026-03-05: Iterations 11-16 completed (ad-hoc approach)
- 2026-03-04: Iterations 7-10 completed
- 2026-02-23: Initial structure with iterations 1-6
