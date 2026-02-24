# Java-Direct Module: Comprehensive Implementation Plan

## Executive Summary

This document describes the implementation approach for the `java-direct` module, which replaces the IntelliJ platform-based Java parsing and resolution with a custom implementation. The goal is to eliminate the IntelliJ platform dependency in the Kotlin compiler while maintaining full Java-Kotlin bidirectional interoperability.

**Status**: Implementation in progress  
**Last Updated**: 2026-02-23

**Important**: See `FIRSESSION_RESOLUTION_ANALYSIS.md` for critical architectural decision about type resolution.

---

## 1. Background and Motivation

### 1.1 High-Level Goal

Remove the IntelliJ platform dependency from the Kotlin compiler. The platform was not designed for compiler use cases and causes:
- Heavy PSI infrastructure overhead
- Complex light classes maintenance
- Limited control over resolution and laziness
- Coupling to platform update cycles
- Potential symbol duplication between Java and Kotlin subsystems

### 1.2 The Java Interoperability Challenge

Kotlin supports **bidirectional interoperability** with Java:
- Java sources can coexist with Kotlin sources in the same project
- References can flow in both directions transitively
- When compiling Kotlin code that references Java sources, binary representations may not exist yet
- The compiler must parse Java sources and extract visible declarations

This means the Kotlin compiler needs "half of a Java compiler" to read and understand Java source declarations.

### 1.3 Current Architecture

**Java Model Layer**: Abstract interfaces defined in:
- `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaElements.kt`
- Key interfaces: `JavaClass`, `JavaMethod`, `JavaField`, `JavaPackage`, `JavaType`, etc.

**Current Implementations**:
1. **PSI-based** (production): `compiler/frontend.common.jvm/` - uses IntelliJ platform PSI
2. **javac-wrapper** (outdated): `compiler/javac-wrapper/` - uses unofficial javac APIs
3. **Binary/Reflection**: For loading from compiled classes

**FIR Integration**: 
- `FirJavaFacade` converts Java Model → FIR declarations
- `JavaSymbolProvider` exposes Java classes to FIR resolution
- Symbol sharing between Java and Kotlin parts via FIR session

### 1.4 Decision: Custom Implementation

After evaluating alternatives (ECJ, javac, custom), the decision is to build a **custom implementation** that:
- Uses **KMP Java Parser** (`org.jetbrains.java.syntax.jvm`) for parsing
- Implements Java Model interfaces directly over AST
- Delegates resolution to **FIR infrastructure** (no light classes!)
- Maintains lazy evaluation for performance
- Provides full control over the pipeline

---

## 2. Architecture Overview

### 2.1 Component Stack

```
┌─────────────────────────────────────────────────────────────┐
│                    Kotlin Resolution (FIR)                  │
├─────────────────────────────────────────────────────────────┤
│                    FirJavaFacade                            │
│              (Java Model → FIR converter)                   │
├─────────────────────────────────────────────────────────────┤
│                    JavaSymbolProvider                        │
│              (exposes Java classes to FIR)                  │
├─────────────────────────────────────────────────────────────┤
│               JavaClassFinderOverAstImpl                     │
│          (file indexing, class lookup, caching)             │
├─────────────────────────────────────────────────────────────┤
│              Java Model Implementation                       │
│  (JavaClassOverAst, JavaMethodOverAst, etc.)               │
│              - Lazy type resolution                          │
│              - Local scope + FIR delegation                  │
├─────────────────────────────────────────────────────────────┤
│                KMP Java Parser                               │
│         (org.jetbrains.java.syntax.jvm)                     │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Key Principles

1. **No modifications to FirJavaFacade**: Work through Java Model interfaces
2. **Lazy evaluation**: Parse and resolve only when needed
3. **FIR delegation**: Use FIR symbol providers for external symbol lookup
4. **Local scope optimization**: Cache declarations from current file
5. **Symbol sharing**: No duplication between Java and Kotlin symbols
6. **No light classes**: Kotlin symbols directly accessible via FIR

---

## 3. Detailed Component Design

### 3.1 File Indexing and Discovery

**Location**: `JavaClassFinderOverAstImpl`

#### 3.1.1 Index Structure

```kotlin
data class FileEntry(
    val path: Path,
    val packageFqName: FqName,
    val topLevelClassNames: MutableSet<String>,  // Can be updated after parsing
    val isPrecise: Boolean = false  // True after actual parsing
)

// Index: package → className → list of files
private val index: MutableMap<FqName, MutableMap<String, MutableList<FileEntry>>>
```

#### 3.1.2 Pre-parsing Strategy

**Purpose**: Build initial index without full parsing for performance

**Approach**:
1. **Regex-based extraction** from raw file text:
   - Extract `package` directive
   - Find class declarations (all variants: `class`, `interface`, `enum`, `record`, `@interface`)
   - Take only first top-level class per file (simplification for initial implementation)

**Regexes** (approximate patterns):
```kotlin
private val PACKAGE_PATTERN = Regex("""^\s*package\s+([\w.]+)\s*;""", RegexOption.MULTILINE)
private val CLASS_PATTERN = Regex(
    """(?:^|\n)\s*(?:public\s+|private\s+|protected\s+|static\s+|final\s+|abstract\s+)*\s*(?:class|interface|enum|@interface|record)\s+(\w+)""",
    RegexOption.MULTILINE
)
```

**Note**: Regexes may produce false positives (e.g., classes in comments), but this is acceptable for the index.

#### 3.1.3 Index Update After Parsing

When a file is actually parsed:
1. Extract precise class names from AST
2. Update corresponding `FileEntry` with precise names
3. Mark entry as `isPrecise = true`
4. Update index mappings

#### 3.1.4 Lookup Flow

```kotlin
fun findClass(request: JavaClassFinder.Request): JavaClass? {
    val classId = request.classId
    val packageFqName = classId.packageFqName
    val topLevelName = classId.relativeClassName.pathSegments().first()
    
    // 1. Check cache
    classCache[classId]?.let { return it }
    
    // 2. Find candidate files from index
    val files = index[packageFqName]?.get(topLevelName.asString()) ?: return null
    
    // 3. Parse file(s) and build Java model
    for (file in files) {
        val javaClass = parseAndBuildClass(file.path, classId) ?: continue
        classCache[classId] = javaClass
        return javaClass
    }
    
    return null
}
```

### 3.2 Java Model Implementation

**Location**: `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/`

#### 3.2.1 Core Classes

**Current prototype** (to be enhanced):
- `JavaClassOverAst` ✓ (basic implementation exists)
- `JavaMethodOverAst` ✓ (exists)
- `JavaFieldOverAst` ✓ (exists)
- `JavaConstructorOverAst` ✓ (exists)
- `JavaTypeParameterOverAst` ✓ (exists)
- `JavaPackageOverAst` ✓ (exists)
- `JavaAnnotationOverAst` ✓ (exists)

**To be added/enhanced**:
- Type resolution mechanism
- Lazy member initialization
- FIR integration for external types

#### 3.2.2 Type Resolution Architecture

**Key Decision**: Type resolution happens in the **FIR layer**, not in the Java Model layer.

See `FIRSESSION_RESOLUTION_ANALYSIS.md` for detailed rationale.

**Problem**: When parsing `class Foo extends Bar<Baz>`, we encounter type references (`Bar`, `Baz`) that need resolution. However, `FirSession` doesn't exist yet when `JavaClass` is created.

**Solution**: Java Model provides **names** and **local structure**, FIR performs **resolution**

##### Java Model Responsibilities

```kotlin
class JavaClassOverAst(
    private val node: JavaSyntaxNode,
    private val source: CharSequence,
    private val localScope: LocalJavaScope,  // For local classes only
    private val imports: JavaImports?,        // For name qualification
    override val outerClass: JavaClass? = null
) : JavaClass {
    
    // Lazy extraction of supertypes (NOT resolution!)
    override val supertypes: Collection<JavaClassifierType> by lazy {
        node.findChildByType("EXTENDS_LIST")
            ?.getChildrenByType("JAVA_CODE_REFERENCE")
            ?.map { createClassifierType(it) }
            ?: emptyList()
    }
    
    private fun createClassifierType(refNode: JavaSyntaxNode): JavaClassifierType {
        return JavaClassifierTypeOverAst(refNode, source, localScope, imports)
    }
}
```

##### JavaClassifierType Implementation

```kotlin
class JavaClassifierTypeOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    private val localScope: LocalJavaScope?,
    private val imports: JavaImports?
) : JavaClassifierType {
    
    // Resolves LOCAL classes only, returns null otherwise
    override val classifier: JavaClassifier? by lazy {
        val simpleName = node.text
        localScope?.findClass(Name.identifier(simpleName))
    }
    
    // Returns fully qualified name when possible, simple name otherwise
    // FIR will use this for resolution if classifier is null
    override val classifierQualifiedName: String by lazy {
        val typeName = node.text
        
        // Already qualified (contains dot)?
        if (typeName.contains('.')) return typeName
        
        // Check simple imports
        imports?.simpleImports?.get(typeName)?.asString()
            ?: typeName  // Return simple name, FIR will handle star imports and java.lang
    }
    
    override val typeArguments: List<JavaType> 
        get() = emptyList()  // TODO: parse type arguments
    
    override val isRaw: Boolean 
        get() = false  // TODO: detect raw types
    
    override val presentableText: String 
        get() = node.text
}
```

##### FIR Layer Resolution

The FIR layer handles all external resolution via `JavaTypeConversion.kt`:

```kotlin
// In compiler/fir/fir-jvm/src/.../JavaTypeConversion.kt (EXISTING CODE)
private fun JavaClassifierType.toConeKotlinTypeForFlexibleBound(
    session: FirSession,
    javaTypeParameterStack: JavaTypeParameterStack,
    // ...
): ConeLookupTagBasedType {
    return when (val classifier = classifier) {
        is JavaClass -> {
            // Use classifier.classId directly
            val classId = classifier.classId!!
            // ... resolve via session.symbolProvider
        }
        is JavaTypeParameter -> {
            // Resolve via javaTypeParameterStack
        }
        null -> {
            // FALLBACK: Parse classifierQualifiedName
            val classId = ClassId.topLevel(FqName(this.classifierQualifiedName))
            classId.constructClassLikeType(...)
        }
    }
}
```

**Key Points**:
1. ✅ Java Model returns `classifier = null` for external types
2. ✅ Java Model provides correct `classifierQualifiedName` 
3. ✅ FIR uses `session.symbolProvider` to resolve external types
4. ✅ FIR handles star imports, java.lang, and package resolution
5. ✅ No FirSession access needed in Java Model!
```

#### 3.2.3 Local Scope Management

**Purpose**: Cache declarations from the currently parsed file to avoid re-resolution

```kotlin
class LocalJavaScope {
    private val classes = mutableMapOf<String, JavaClass>()
    private val typeParameters = mutableMapOf<String, JavaTypeParameter>()
    
    fun addClass(name: String, javaClass: JavaClass) {
        classes[name] = javaClass
    }
    
    fun findClass(name: String): JavaClass? = classes[name]
    
    // Built during parsing by walking AST
    companion object {
        fun buildFromAST(root: JavaSyntaxNode, source: CharSequence): LocalJavaScope {
            val scope = LocalJavaScope()
            root.getChildrenByType("CLASS").forEach { classNode ->
                val name = classNode.findChildByType("IDENTIFIER")?.text
                if (name != null) {
                    // Eagerly create JavaClass (or store AST node for lazy creation)
                    scope.addClass(name, JavaClassOverAst(classNode, source, scope, ...))
                }
            }
            return scope
        }
    }
}
```

#### 3.2.4 No FIR Symbol Wrappers Needed

**Previous approach** (now obsolete): Wrap FirRegularClassSymbol as JavaClass.

**Current approach**: Not needed! FIR handles resolution internally without round-tripping through Java Model.

**Why**:
- Java Model provides type names via `classifierQualifiedName`
- FIR resolves types to FirTypeRef directly
- No need to convert FirRegularClassSymbol back to JavaClass
- Eliminates circular dependency and complexity

**Exception**: If Java Model needs to query FIR symbols for validation/diagnostics, use FIR APIs directly (not through Java Model wrapper).

### 3.3 Type Parameter Stack

**Purpose**: Track type parameters during nested class traversal for FIR conversion.

The FIR implementation uses `MutableJavaTypeParameterStack` (in `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/MutableJavaTypeParameterStack.kt`).

**Integration**: 
- Stack is built in **FIR layer** during `FirJavaFacade.convertJavaClassToFir()`
- Java Model just needs to expose `typeParameters` correctly
- FIR maps `JavaTypeParameter` → `FirTypeParameterSymbol`

```kotlin
// In FirJavaFacade (EXISTING CODE - no changes needed)
fun convertJavaClassToFir(
    classSymbol: FirRegularClassSymbol,
    parentClassSymbol: FirRegularClassSymbol?,
    javaClass: JavaClass,
): FirJavaClass {
    val javaTypeParameterStack = MutableJavaTypeParameterStack()
    
    if (parentClassSymbol != null) {
        val parentStack = (parentClassSymbol.fir as FirJavaClass).classJavaTypeParameterStack
        javaTypeParameterStack.addStack(parentStack)
    }
    
    // Add this class's type parameters
    javaClass.typeParameters.forEach { javaTypeParam ->
        val firTypeParam = javaTypeParam.toFirTypeParameter(classSymbol, moduleData)
        javaTypeParameterStack.addParameter(javaTypeParam, firTypeParam.symbol)
    }
    
    // Stack is now available for type resolution
    // ...
}
```

**Java Model responsibility**: Simply return type parameters from AST

```kotlin
class JavaClassOverAst {
    override val typeParameters: List<JavaTypeParameter> by lazy {
        node.findChildByType("TYPE_PARAMETER_LIST")
            ?.getChildrenByType("TYPE_PARAMETER")
            ?.map { JavaTypeParameterOverAst(it, source) }
            ?: emptyList()
    }
}
```

### 3.4 Import Handling

**Problem**: Java resolution depends on imports (single-type and on-demand)

**Solution**: Extract imports during initial parsing

```kotlin
data class JavaImports(
    val simpleImports: Map<String, FqName>,  // SimpleName → FullyQualifiedName
    val starImports: List<FqName>             // Package names for * imports
)

fun extractImports(root: JavaSyntaxNode): JavaImports {
    val simple = mutableMapOf<String, FqName>()
    val star = mutableListOf<FqName>()
    
    root.children.filter { it.type.toString() == "IMPORT_STATEMENT" }.forEach { importNode ->
        val isStatic = importNode.children.any { it.type.toString() == "STATIC_KEYWORD" }
        if (isStatic) return@forEach  // Skip static imports for now
        
        val ref = importNode.findChildByType("JAVA_CODE_REFERENCE")?.text ?: return@forEach
        
        if (ref.endsWith(".*")) {
            star.add(FqName(ref.removeSuffix(".*")))
        } else {
            val simpleName = ref.substringAfterLast('.')
            simple[simpleName] = FqName(ref)
        }
    }
    
    return JavaImports(simple, star)
}
```

**Usage in Java Model**:
```kotlin
class JavaClassifierTypeOverAst(
    node: JavaSyntaxNode,
    source: CharSequence,
    private val localScope: LocalJavaScope?,
    private val imports: JavaImports?
) : JavaClassifierType {
    
    override val classifierQualifiedName: String by lazy {
        val typeName = node.text
        
        // Already qualified?
        if (typeName.contains('.')) return typeName
        
        // Check simple imports
        imports?.simpleImports?.get(typeName)?.asString()
            ?: typeName  // Keep simple name, FIR will resolve
    }
}
```

**FIR handles star imports automatically** - no need to implement in Java Model.

---

## 4. Annotation Arguments and Constant Evaluation

### 4.1 Current State

- `javac-wrapper` has `ConstantEvaluator` that evaluates Java constant expressions
- FIR has `FirConstantEvaluationProcessor` for Kotlin constants
- Annotation arguments require constant evaluation per Java semantics

### 4.2 Implementation Strategy

**Phase 1** (Initial): **Skip complex constant evaluation**
- Support only literal values (strings, numbers, booleans)
- Return `null` for complex expressions
- This covers ~80% of real-world annotations

```kotlin
override val annotationParameterDefaultValue: JavaAnnotationArgument? 
    get() {
        val defaultNode = node.findChildByType("ANNOTATION_METHOD_DEFAULT") ?: return null
        val valueNode = defaultNode.findChildByType("ANNOTATION_VALUE") ?: return null
        
        // Only handle literals for now
        return when {
            valueNode.findChildByType("LITERAL") != null -> 
                literalToAnnotationArgument(valueNode)
            else -> null  // Complex expression, skip for now
        }
    }
```

**Phase 2** (Later): **Reuse FirConstantEvaluationProcessor**
- Build minimal FIR representation for annotation default expressions
- Pass through `FirConstantEvaluationProcessor`
- Extract result and convert back to `JavaAnnotationArgument`

Reference implementation: Scripting with LightTree (mentioned in interview)

### 4.3 Deferred Work

Mark as TODO for future iterations:
- Array values in annotations
- Enum constants in annotations  
- Class literals (`Foo.class`)
- Nested annotations
- Binary operations in constants

---

## 5. Integration with Compilation Pipeline

### 5.1 Entry Points

#### 5.1.1 JavaClassFinderFactory Extension

**File**: `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaDirectComponentRegistrar.kt`

```kotlin
class JavaClassFinderOverAstFactory(
    private val configuration: CompilerConfiguration
): JavaClassFinderFactory {
    
    override fun createJavaClassFinder(
        scope: AbstractProjectFileSearchScope,
        annotationProvider: JavaAnnotationProvider?,
    ): JavaClassFinder {
        // Extract source roots from scope
        val sourceRoots = extractSourceRoots(scope)
        
        return JavaClassFinderOverAstImpl(
            sourceRoots = sourceRoots,
            annotationProvider = annotationProvider
        )
    }
    
    private fun extractSourceRoots(scope: AbstractProjectFileSearchScope): List<Path> {
        // Analyze scope and extract source directories
        // This may require scope inspection or configuration parameter
        TODO("Extract source roots from scope")
    }
}
```

**Registration**: Via `CompilerPluginRegistrar`
```kotlin
class JavaDirectComponentRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        JavaClassFinderFactory.registerExtension(
            JavaClassFinderOverAstFactory(configuration)
        )
    }
}
```

#### 5.1.2 VfsBasedProjectEnvironment Override

**File**: `compiler/java-direct/testFixtures/.../VfsBasedProjectEnvironmentOverAst.kt`

```kotlin
class VfsBasedProjectEnvironmentOverAst(
    project: Project,
    fileSystem: VirtualFileSystem,
    getPackagePartProviderFn: (GlobalSearchScope) -> PackagePartProvider,
    private val librariesScope: AbstractProjectFileSearchScope,
    private val javaSourceRoots: List<Path>,
) : VfsBasedProjectEnvironment(project, fileSystem, getPackagePartProviderFn) {

    override fun getFirJavaFacade(
        firSession: FirSession,
        baseModuleData: FirModuleData,
        fileSearchScope: AbstractProjectFileSearchScope,
    ): FirJavaFacadeForSource {
        // For libraries: use default (PSI/bytecode)
        if (fileSearchScope === librariesScope) {
            return super.getFirJavaFacade(firSession, baseModuleData, fileSearchScope)
        }

        // For sources: use our implementation
        val javaClassFinder = JavaClassFinderOverAstImpl(javaSourceRoots)
        return FirJavaFacadeForSource(firSession, baseModuleData, javaClassFinder)
    }
}
```

### 5.2 Test Infrastructure Integration

#### 5.2.1 Test Configuration

**File**: `compiler/java-direct/testFixtures/.../AbstractJavaUsingAstTest.kt`

```kotlin
abstract class AbstractJavaUsingAstTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        // Configure to use FIR + LightTree
        configureFirParser(FirParser.LightTree)
        
        // Use custom project environment
        useConfigurators(::JavaDirectProjectEnvironmentConfigurator)
        
        // Standard test handlers
        configureFirHandlersStep {
            setupHandlersForDiagnosticTest()
            useHandlers(::NoFirCompilationErrorsHandler)
        }
        
        // ... rest of configuration
    }
}
```

#### 5.2.2 Project Environment Configurator

```kotlin
class JavaDirectProjectEnvironmentConfigurator(testServices: TestServices) : 
    EnvironmentConfigurator(testServices) {
    
    override fun configureCompilerConfiguration(
        configuration: CompilerConfiguration, 
        module: TestModule
    ) {
        // Enable java-direct plugin
        configuration.addCompilerPlugin(JavaDirectComponentRegistrar())
        
        // Or: pass source roots explicitly for test environment
        configuration.put(
            JAVA_SOURCE_ROOTS_KEY, 
            extractJavaSourceRoots(module)
        )
    }
}
```

### 5.3 CLI Integration

**Compiler Argument**: `--java-direct` or `-Xjava-direct`

```kotlin
object CompilerArguments {
    val USE_JAVA_DIRECT = CompilerConfigurationKey<Boolean>("use java-direct implementation")
}

// In CLI argument parser
if (hasArgument("--java-direct")) {
    configuration.put(USE_JAVA_DIRECT, true)
    configuration.addCompilerPlugin(JavaDirectComponentRegistrar())
}
```

---

## 6. Testing Strategy

### 6.1 Test Phases

#### Phase 1: Unit Tests
**Goal**: Test individual components in isolation

**Tests**:
1. **Parser tests** (already exist: `JavaParsingTest`)
   - Verify AST structure for various Java syntax
   
2. **Java Model tests** (new)
   - Test `JavaClassOverAst` construction
   - Verify member extraction (methods, fields, constructors)
   - Test visibility, modifiers, annotations
   
3. **Index tests** (new)
   - Test pre-parsing regex extraction
   - Verify index structure
   - Test lookup by ClassId
   
4. **Type resolution tests** (new)
   - Test local scope resolution
   - Test import-based resolution
   - Test FIR delegation (may need mocking)

#### Phase 2: Integration Tests
**Goal**: Test compilation of simple Kotlin+Java projects

**Test Suite**: `AbstractJavaUsingAstTest` and `AbstractJavaUsingAstBoxTest`

**Simple test cases** (start with these):

```java
// test/data/Simple.java
package test;
public class Simple {
    public String field = "hello";
    public int getNumber() { return 42; }
}
```

```kotlin
// test/data/simple.kt
package test
fun box(): String {
    val s = Simple()
    return if (s.field == "hello" && s.getNumber() == 42) "OK" else "FAIL"
}
```

**Progressive complexity**:
1. Simple class with fields and methods ✓
2. Class with supertype (Java → Java)
3. Generic classes
4. Inner classes
5. Annotations with simple arguments
6. Kotlin referencing Java referencing Kotlin (circular)

#### Phase 3: Existing Test Suites
**Goal**: Run existing compiler tests with new implementation

**Approach**:
1. Identify tests involving Java sources (grep for `.java` files in testData)
2. Run with `-Xjava-direct` flag
3. Compare results with PSI-based implementation
4. Fix discrepancies

**High-priority test suites**:
- `compiler/testData/codegen/box/` (blackbox tests)
- `compiler/testData/diagnostics/tests/` (error reporting)
- Tests in `compiler/tests-gen/` involving Java

### 6.2 Testing Metrics

**Coverage goals**:
- ☐ Parser: 90%+ (AST construction)
- ☐ Java Model: 80%+ (all interface methods)
- ☐ Type resolution: 70%+ (main paths covered)
- ☐ Integration: 50+ passing box tests

**Performance benchmarks**:
- Compare compilation time: PSI vs java-direct
- Measure memory usage
- Profile lazy evaluation effectiveness

---

## 7. Implementation Roadmap

### Milestone 1: Foundation
**Goal**: Working prototype with simplest test passing

**Tasks**:
- [x] Implement regex-based pre-parsing and indexing
  - [x] Extract package directive
  - [x] Extract class names (first class per file)
  - [x] Build index structure
  
- [x] Enhance `JavaClassFinderOverAstImpl`
  - [x] Implement proper caching
  - [x] Integrate with index
  - [x] Support nested class lookup
  
- [x] Implement local scope extraction
  - [x] Walk AST after parsing
  - [x] Cache local declarations
  
- [x] Implement basic type resolver
  - [x] Local scope resolution
  - [x] Current package resolution
  - [x] java.lang resolution
  
- [x] Wire to test infrastructure
  - [x] Implement `JavaClassFinderOverAstFactory`
  - [x] Configure `VfsBasedProjectEnvironmentOverAst`
  - [x] Make simplest test pass

**Success criteria**: 
- ✓ Simple.java + simple.kt compiles and runs
- ✓ No FIR errors for basic class access

### Milestone 2: Resolution & FIR Integration
**Goal**: Complete type resolution mechanism

**Tasks**:
- [ ] Implement import tracking
  - [ ] Extract simple imports
  - [ ] Extract star imports
  - [ ] Integrate into resolution
  
- [ ] Implement FIR delegation
  - [ ] Query `FirSession.symbolProvider`
  - [ ] Convert `FirRegularClassSymbol` → `JavaClass` wrapper
  - [ ] Handle resolution failures gracefully
  
- [ ] Implement generic type handling
  - [ ] Type parameter resolution
  - [ ] Type argument substitution
  - [ ] Wildcard types
  
- [ ] Implement supertype resolution
  - [ ] Lazy resolution in `JavaClassOverAst.supertypes`
  - [ ] Handle circular dependencies
  
- [ ] Test with moderate complexity
  - [ ] Inheritance hierarchies
  - [ ] Generic classes
  - [ ] Java → Java → Kotlin chains

**Success criteria**:
- ✓ 20+ box tests passing
- ✓ Generic classes work correctly
- ✓ Kotlin can extend Java classes

### Milestone 3: Annotations & Constants
**Goal**: Support annotation processing

**Tasks**:
- [ ] Implement literal annotation arguments
  - [ ] Strings, numbers, booleans
  - [ ] Return from `annotationParameterDefaultValue`
  
- [ ] (Optional) Implement constant evaluator
  - [ ] Reuse `FirConstantEvaluationProcessor`
  - [ ] Or implement simple evaluator for common cases
  
- [ ] Handle annotation metadata
  - [ ] @Target, @Retention
  - [ ] Custom annotations
  
- [ ] Test annotation-heavy code
  - [ ] Spring/JakartaEE-style annotations
  - [ ] Annotation processors (if relevant)

**Success criteria**:
- ✓ Simple annotations work
- ✓ Annotation arguments accessible
- ✓ No regressions in annotation-based tests

### Milestone 4: Production Readiness
**Goal**: Feature-complete, stable, tested

**Tasks**:
- [ ] Support modern Java features
  - [ ] Records (Java 16+)
  - [ ] Sealed classes (Java 17+)
  - [ ] Pattern matching in instanceof (Java 16+)
  
- [ ] Performance optimization
  - [ ] Profile hot paths
  - [ ] Optimize caching strategy
  - [ ] Reduce parsing overhead
  
- [ ] Error handling & resilience
  - [ ] Graceful handling of malformed Java
  - [ ] Proper diagnostic reporting
  - [ ] Recovery from resolution failures
  
- [ ] Run full test suite
  - [ ] Enable for all compiler tests
  - [ ] Achieve >95% pass rate vs PSI
  - [ ] Fix remaining failures
  
- [ ] Documentation
  - [ ] API documentation
  - [ ] Architecture guide
  - [ ] Migration guide

**Success criteria**:
- ✓ All targeted Java versions supported (8-21+)
- ✓ Test pass rate ≥ 95%
- ✓ Performance comparable to PSI
- ✓ Ready for opt-in use in production

### Milestone 5: Gradual Rollout
**Goal**: Deploy to users safely

**Tasks**:
- [ ] CLI flag implementation (`-Xjava-direct`)
- [ ] Gradle plugin support
- [ ] Internal dogfooding (compile Kotlin repo itself)
- [ ] Beta testing with select users
- [ ] Make default in a future Kotlin version

---

## 8. Known Limitations and Future Work

### 8.1 Current Limitations

**Not Supported Initially**:
- Complex constant expressions in annotations
- Static imports
- Method bodies analysis (not needed for declarations)
- Some edge cases in generic type resolution

**Workarounds**:
- Use PSI-based implementation as fallback (via configuration)
- Detect unsupported patterns and report clear errors

### 8.2 Future Enhancements

**Post-MVP Features**:
1. **Incremental compilation support**
   - Track file dependencies
   - Invalidate cache on changes
   - Integrate with Kotlin incremental compilation

2. **IDE integration** (maybe never needed)
   - Analysis API might continue using PSI
   - But could offer java-direct as alternative for standalone analysis

3. **Performance optimizations**
   - Parallel parsing of multiple files
   - More sophisticated caching
   - Lazy parsing of method bodies (even though we don't use them)

4. **Extended Java support**
   - Lambda expressions (if needed for type inference)
   - Method references
   - Annotation processing integration

### 8.3 Non-Goals

**Explicitly Out of Scope**:
- Full Java compiler functionality (no bytecode generation)
- Java-to-Java compilation
- Support for javadoc processing (unless needed for deprecation)
- 100% bug-for-bug compatibility with javac semantics

---

## 9. Technical Risks and Mitigations

### 9.1 Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| FIR integration complexities | High | Medium | Start with simple cases, iterate |
| Performance regression | High | Low | Profile early, optimize iteratively |
| Generic type resolution edge cases | Medium | Medium | Comprehensive test coverage |
| Regex-based pre-parsing inaccuracies | Low | High | Accept false positives, fix during parsing |
| Circular dependencies between Java files | Medium | Low | Proper lazy evaluation |

### 9.2 Mitigation Strategies

1. **Incremental development**: Start simple, add complexity gradually
2. **Parallel running**: Keep PSI implementation, compare results
3. **Extensive testing**: Unit, integration, and regression tests
4. **Performance monitoring**: Regular benchmarks
5. **Fallback mechanism**: Allow switching back to PSI if issues arise

---

## 10. Open Questions and TODOs

### 10.1 Design Questions

- [ ] **Q1**: Should `JavaClassFromFirSymbol` wrapper be heavyweight or lightweight?
  - Option A: Full wrapper with lazy conversion
  - Option B: Extend Java Model to accept FIR symbols directly
  - **Decision needed by**: Milestone 2

- [ ] **Q2**: How to handle error recovery in parsing?
  - Option A: Fail fast on parse errors
  - Option B: Build partial Java Model
  - **Current**: Fail fast (decided)

- [ ] **Q3**: Should we support all Java features PSI supports from day 1?
  - Option A: Yes (longer initial development)
  - Option B: Start with Java 8-11, add features incrementally
  - **Current**: Start with Java 8-9, add incrementally (decided)

### 10.2 Implementation TODOs

**High Priority**:
- [ ] Determine exact format for source root configuration
- [ ] Decide on error type representation for unresolved types
- [ ] Design cache invalidation strategy
- [ ] Implement proper lifecycle management for JavaClassFinder instances

**Medium Priority**:
- [ ] Static import handling
- [ ] Type annotation support (`@Nullable`, etc.)
- [ ] Better handling of type parameter bounds

**Low Priority**:
- [ ] Optimization of pre-parsing regex
- [ ] Support for malformed Java (error recovery)
- [ ] Incremental compilation hooks

---

## 11. References

### 11.1 Key Files

**Interfaces**:
- `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaElements.kt`
- `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/JavaClassFinder.kt`

**Current Implementations**:
- `compiler/frontend.common.jvm/` (PSI-based)
- `compiler/javac-wrapper/` (javac-based, reference only)

**FIR Integration**:
- `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/FirJavaFacade.kt`
- `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaSymbolProvider.kt`
- `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaTypeConversion.kt`

**Prototype**:
- `compiler/java-direct/src/` (current implementation)

### 11.2 Documentation

- Design meeting notes: `compiler/java-direct/DM 20-11-25 - Approaches to Java sources parsing and resolution.md`
- Initial analysis: `compiler/java-direct/initial-analysis.md`
- Project guidelines: `.ai/guidelines.md`

### 11.3 Related Issues

- **KT-70023**: Consider getting rid of KotlinJavaPsiFacade
- **OSIP-191**: Get rid of IJ platform dependency in Compiler Frontend

---

## 12. Appendix: Code Examples

### 12.1 Complete Example: Simple Type Resolution

```kotlin
// Example: Resolving `List<String>` in Java source

// Input AST node for type reference
val typeNode: JavaSyntaxNode  // represents "List<String>"

// Step 1: Extract type structure
val typeName = typeNode.findChildByType("IDENTIFIER")?.text  // "List"
val typeArgs = typeNode.findChildByType("TYPE_ARGUMENTS")
    ?.getChildrenByType("TYPE")  // [<String>]

// Step 2: Resolve base type
val baseClassifier = resolver.resolveType(typeName, contextClass)
// → Queries FIR → finds java.util.List (from imports)

// Step 3: Resolve type arguments
val resolvedArgs = typeArgs.map { argNode ->
    resolver.resolveType(argNode.text, contextClass)
}
// → [java.lang.String]

// Step 4: Construct JavaClassifierType
val result = JavaClassifierTypeOverAst(
    classifier = baseClassifier,
    typeArguments = resolvedArgs,
    isNullable = false  // Java types are nullable by default in Kotlin
)
```

### 12.2 Example: Lazy Supertype Resolution

```kotlin
class JavaClassOverAst(...) : JavaClass {
    override val supertypes: Collection<JavaClassifierType> by lazy {
        val extendsTypes = node.findChildByType("EXTENDS_LIST")
            ?.getChildrenByType("JAVA_CODE_REFERENCE")
            ?.map { parseTypeReference(it) }
            ?: emptyList()
        
        val implementsTypes = node.findChildByType("IMPLEMENTS_LIST")
            ?.getChildrenByType("JAVA_CODE_REFERENCE")
            ?.map { parseTypeReference(it) }
            ?: emptyList()
        
        extendsTypes + implementsTypes
    }
    
    private fun parseTypeReference(refNode: JavaSyntaxNode): JavaClassifierType {
        // This is called lazily only when supertypes are accessed
        return resolver.resolveType(refNode.text, this)
    }
}

// Usage:
val myClass: JavaClass = findClass(ClassId(...))
// At this point, supertypes not yet resolved

// Later, when FirJavaFacade converts to FIR:
val superTypes = myClass.supertypes  // NOW resolution happens
```

---

## Document Change Log

- 2026-02-23: Major update to section 3.2.2-3.2.4: Type resolution now happens in FIR layer, not Java Model (see FIRSESSION_RESOLUTION_ANALYSIS.md for detailed rationale)
- 2026-02-23: Updated section 3.3: Type parameter stack is built in FIR layer during conversion
- 2026-02-23: Removed obsolete JavaTypeResolver and JavaClassFromFirSymbol wrapper descriptions
- 2026-02-23: Updated section 3.4: Import handling simplified - Java Model qualifies names, FIR resolves
- 2026-02-10: Updated terminology to match FIR naming conventions (`singleTypeImports` → `simpleImports`, `onDemandImports` → `starImports`); removed time estimates from milestones
- 2026-02-09: Initial version created based on interview with requirements provider

---

**End of Implementation Plan**
