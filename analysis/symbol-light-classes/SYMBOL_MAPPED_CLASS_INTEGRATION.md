# Symbol-Based Mapped Class Integration

## Overview

This document describes the alternative implementations of mapped class utilities that use KaSymbol abstractions instead of PsiMethod and PsiSubstitutor, aligning with the Symbol light classes architecture.

## Files Created

1. **SymbolLightMethodForMappedClassV2.kt** - Alternative method implementation
2. **mappedClassUtilsV2.kt** - Alternative utility functions

## Key Architectural Differences

### Original Implementation (SymbolLightMethodForMappedClass)

```kotlin
class SymbolLightMethodForMappedClass(
    private val javaMethod: PsiMethod,           // Direct PSI dependency
    private val substitutor: PsiSubstitutor,     // PSI-based type substitution
    private val substituteObjectWith: PsiType?,  // Already converted to PsiType
    private val providedSignature: MethodSignature?  // PsiType-based signature
)
```

**Issues:**
- Stores PsiMethod directly, breaking Symbol light classes pattern
- Uses PsiSubstitutor for type substitution
- Type conversion happens eagerly at construction time
- No KaSymbolPointer, making it inconsistent with other Symbol light classes

### New Implementation (SymbolLightMethodForMappedClassV2)

```kotlin
class SymbolLightMethodForMappedClassV2(
    private val functionSymbolPointer: KaSymbolPointer<KaNamedFunctionSymbol>,  // Symbol pointer
    private val objectSubstitution: KaType?,      // Kotlin type for substitution
    private val providedSignature: KaMethodSignature?  // Can use KaType internally
)
```

**Benefits:**
- Uses KaSymbolPointer, consistent with SymbolLightMethod, SymbolLightSimpleMethod
- Stores type information as KaType
- PsiType conversion happens lazily via asPsiType()
- Follows the established pattern in Symbol light classes

## Type Substitution Strategy

### Original Approach
1. Find Java collection PsiClass
2. Create PsiSubstitutor with type parameter mappings
3. Substitute PsiTypes eagerly
4. Store substituted PsiTypes in the method

### New Approach
1. Find Java collection as KaNamedClassSymbol
2. Extract type arguments from KaClassType (e.g., List<String> → String)
3. Store objectSubstitution as KaType
4. In parameter/return type computation:
   ```kotlin
   private fun KaSession.substituteIfNeeded(kaType: KaType): KaType {
       if (objectSubstitution != null && kaType.isAny && !kaType.isMarkedNullable) {
           return objectSubstitution  // Replace Object with concrete type
       }
       return kaType
   }
   ```
5. Convert to PsiType lazily when needed

## Key Functions Mapping

### mappedClassUtils.kt → mappedClassUtilsV2.kt

| Original Function | New Function | Key Changes |
|------------------|--------------|-------------|
| `processPossiblyMappedMethod` | `processPossiblyMappedMethodV2` | Uses `tryToMapKotlinCollectionMethodToJavaMethodSymbol` to get KaNamedFunctionSymbol instead of PsiMethod |
| `generateJavaCollectionMethodStubsIfNeeded` | `generateJavaCollectionMethodStubsIfNeededV2` | Works with KaNamedClassSymbol for Java collections |
| `createPsiSubstitutor` | `findObjectSubstitutionType` | Returns KaType instead of creating PsiSubstitutor |
| `tryToMapKotlinCollectionMethodToJavaMethod` | `tryToMapKotlinCollectionMethodToJavaMethodSymbol` | Returns KaNamedFunctionSymbol by looking up in symbol's memberScope |

## Integration Steps

### Phase 1: Parallel Implementation (Current)
- ✅ Create V2 implementations alongside original
- ✅ Keep original implementations unchanged
- Document differences and benefits

### Phase 2: Testing & Validation
- Add unit tests comparing behavior of V1 vs V2
- Test edge cases:
  - Generic type substitution (List<String>, Map<K,V>)
  - Special signatures (remove, get, contains)
  - Erased vs non-erased signatures
  - Map entry methods (getKey, getValue)
  - Size property with different ABI names

### Phase 3: Migration
- Update call sites to use V2 implementations
- Possible locations:
  - `SymbolLightClassForClassOrObject.createMethods()`
  - Collection-specific method generation

### Phase 4: Cleanup
- Remove original implementations
- Rename V2 classes to remove suffix
- Update documentation

## Technical Details

### Method Signature Handling

**Original:**
```kotlin
data class MethodSignature(
    val parameterTypes: List<PsiType>,
    val returnType: PsiType
)
```

**New:**
```kotlin
data class KaMethodSignature(
    val parameterTypes: List<PsiType>,  // Pre-computed for special cases
    val returnType: PsiType
)
```

Note: For now, KaMethodSignature still uses PsiType for simplicity. A future enhancement could make it use KaType and convert lazily.

### Special Cases

1. **Map Methods (get, containsKey, remove)**
   - Original: Creates PsiSubstitutor with K, V mappings
   - New: Extracts K, V as KaType from KaClassType.typeArguments

2. **Erased Signatures (contains, remove with Object)**
   - Original: Uses substitutor with Object
   - New: Passes null for objectSubstitution

3. **Size Property (size() → getSize())**
   - Both implementations handle this identically
   - Create final bridge for Java name, abstract method for Kotlin name

## Example: List<String>.contains

### Original Flow
1. Find java.util.Collection PsiClass
2. Create substitutor: {E → String as PsiType}
3. Find contains(Object) PsiMethod
4. Substitute: contains(Object) → contains(String) via PsiSubstitutor
5. Store PsiMethod + PsiSubstitutor

### New Flow
1. Find java.util.Collection as KaNamedClassSymbol
2. Extract type argument: List<String> → String as KaType
3. Find contains function as KaNamedFunctionSymbol
4. Store functionSymbolPointer + objectSubstitution = String (KaType)
5. When getParameterList() called:
   - Get parameter type from function symbol (Object → kotlin.Any)
   - Check if isAny && !nullable → substitute with String (KaType)
   - Convert to PsiType via asPsiType()

## Benefits of New Approach

1. **Consistency**: Follows same patterns as SymbolLightMethod, SymbolLightSimpleMethod
2. **Laziness**: Type conversion only happens when needed
3. **Maintainability**: Less coupling with PSI infrastructure
4. **Performance**: Avoid creating PsiSubstitutor eagerly
5. **Testability**: KaType easier to test than PsiType
6. **Future-proof**: Easier to extend with new type system features

## Potential Issues & Solutions

### Issue 1: PsiMethod.getSignature() compatibility
**Problem**: Existing code may call getSignature(PsiSubstitutor)
**Solution**: V2 implements same PsiMethod interface, works with any PsiSubstitutor

### Issue 2: Type parameter handling
**Problem**: Complex type parameters with bounds
**Solution**: KaType preserves full type information, asPsiType() handles conversion

### Issue 3: Performance of symbol lookups
**Problem**: Looking up methods in memberScope vs getting from PsiClass
**Solution**: Both are cached; memberScope is indexed

## Next Steps

1. Add comprehensive tests for V2 implementations
2. Profile performance comparing V1 vs V2
3. Identify all call sites that need migration
4. Create migration plan with backward compatibility
5. Consider renaming KaMethodSignature to use KaType instead of PsiType
