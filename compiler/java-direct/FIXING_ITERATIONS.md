# Java-Direct: Fixing Iterations

## Document Purpose

This document contains iteration plans for the `java-direct` module.

**Prerequisites**: Read `AGENT_INSTRUCTIONS.md` before starting any iteration  
**Status**: Iteration 18 complete — 1100/1166 box tests passing (94.3%)  
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

### Current Status After Iteration 17b: 1092/1166 box tests (93.7%)

| Category | Count | Example Tests |
|----------|-------|---------------|
| Inner/Nested Classes | ~8 | `testBoundInnerConstructorRef`, `testInheritedInnerAndNested` |
| TYPE_USE on Type Args | 5 | `testJavaCollectionOfNotNullToTypedArrayFailFast` |
| Wildcards/Projections | 3 | `testInheritanceWithWildcard`, `testJavaGenericSynthProperty` |
| Visibility Issues | 2 | `testContractAndRawField` |
| Annotation Edge Cases | ~10 | `testJavaAnnotation`, `testConstValAsAnnotationArgumentInJava` |
| Enum Handling | ~6 | `testJavaEnumValues`, `testEnumEntriesFromJava` |
| Reflection/Metadata | ~5 | `testDelegatedMembers`, `testCallableReferenceToJavaField` |
| Other Edge Cases | ~35 | Various |

### Realistic Expectations for Iterations 18-21

| Iteration | Target | Expected Tests Fixed |
|-----------|--------|---------------------|
| 18 | Nested Class Resolution | ~8-10 |
| 19 | TYPE_USE on Type Args | ~5 |
| 20 | Wildcard Edge Cases | ~3-5 |
| 21 | Visibility Issues | ~3 |

**Projected Status**: ~1115/1166 (~95.6%)

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
