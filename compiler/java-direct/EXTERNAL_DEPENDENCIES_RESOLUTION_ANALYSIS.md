# External Dependencies Resolution Analysis

## Executive Summary

This document analyzes how the Kotlin compiler resolves external dependencies (binary Kotlin classes, binary Java classes from JARs, and Java sources) and identifies the IntelliJ Platform dependencies involved. The goal is to understand the architecture to enable the `java-direct` module to work correctly and eventually remove platform dependencies.

**Key Finding**: The current `java-direct` implementation only replaces the `JavaClassFinder` for Java *sources*, but the resolution of external symbols (from JARs, JDK, etc.) still relies on the full platform-dependent infrastructure. When Kotlin code references types like `String`, `Object`, or any library class, the resolution path goes through `VirtualFileFinder` → `JvmDependenciesIndex` → `VirtualFile` → platform services.

---

## 1. Architecture Overview

### 1.1 Two Parallel Resolution Paths

The Kotlin compiler has **two main symbol resolution paths** for JVM:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           FIR Symbol Resolution                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Path 1: SOURCE SESSION (for current module's sources)                      │
│  ─────────────────────────────────────────────────────────────────────────  │
│  FirSourceElementSymbolProvider                                             │
│       ↓                                                                     │
│  JavaSymbolProvider  ←──  FirJavaFacade  ←──  JavaClassFinder              │
│       │                                            │                        │
│       │                              ┌─────────────┴─────────────┐          │
│       │                              │                           │          │
│       │                     JavaClassFinderImpl         JavaClassFinderOverAstImpl│
│       │                     (PSI-based, current)        (java-direct, new)  │
│       │                              │                           │          │
│       │                              ↓                           ↓          │
│       │                     KotlinJavaPsiFacade         Direct AST parsing  │
│       │                     (platform-dependent)        (platform-free)     │
│       ↓                                                                     │
│  [Kotlin sources are handled separately by FirProvider]                     │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Path 2: LIBRARY SESSION (for dependencies from classpath)                  │
│  ─────────────────────────────────────────────────────────────────────────  │
│  JvmClassFileBasedSymbolProvider                                            │
│       │                                                                     │
│       ├──→ KotlinClassFinder (for Kotlin classes with @Metadata)           │
│       │         │                                                           │
│       │         ↓                                                           │
│       │    VirtualFileFinder (abstract)                                     │
│       │         │                                                           │
│       │         ↓                                                           │
│       │    CliVirtualFileFinder ──→ JvmDependenciesIndex ──→ VirtualFile   │
│       │                                                                     │
│       └──→ FirJavaFacade (for Java classes without @Metadata)              │
│                 │                                                           │
│                 ↓                                                           │
│            JavaClassFinder ──→ (same as source session)                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Key Insight: The Missing Piece

The `java-direct` module currently replaces only `JavaClassFinder` in the **source session** via `VfsBasedProjectEnvironmentOverAst.getFirJavaFacade()`. However:

1. **Library session** still uses `JvmClassFileBasedSymbolProvider` with `VirtualFileFinder`
2. **KotlinClassFinder** (used by `JvmClassFileBasedSymbolProvider`) is created via `VirtualFileFinderFactory`
3. All standard library types (`kotlin.String`, `kotlin.Int`, etc.) are resolved through the library session
4. Even in the source session, when `JavaSymbolProvider` encounters a type reference to an external class (e.g., `java.lang.String`), the FIR resolution eventually queries the library session's symbol providers

---

## 2. Detailed Resolution Paths

### 2.1 Path: FQN → KotlinClassFinder → VirtualFile → Kotlin Binary Class

**Entry Point**: `JvmClassFileBasedSymbolProvider.extractClassMetadata()`

```
1. extractClassMetadata(classId: ClassId)
   │
   ├─► javaFacade.hasTopLevelClassOf(classId)  // Quick check if class might exist
   │
   └─► kotlinClassFinder.findKotlinClassOrContent(classId, metadataVersion)
       │
       ├─► VirtualFileFinder.findKotlinClassOrContent(classId)
       │   │
       │   └─► findVirtualFileWithHeader(classId)
       │       │
       │       └─► CliVirtualFileFinder.findBinaryOrSigClass(classId)
       │           │
       │           └─► JvmDependenciesIndex.findClasses(classId, rootTypes) { dir, _ ->
       │               │   dir.findChild("$simpleName.class")  // VirtualFile operation!
       │               │   dir.findChild("$simpleName.sig")    // For ct.sym files
       │               └─► }
       │
       └─► KotlinBinaryClassCache.getKotlinBinaryClassOrClassFileContent(file, metadataVersion)
           │
           └─► VirtualFileKotlinClass.create(file, metadataVersion, fileContent)
               │
               ├─► file.contentsToByteArray()  // VirtualFile I/O
               │
               └─► ClassReader(byteContent)  // ASM parsing of class file
                   │
                   └─► Extracts @Metadata annotation → KotlinClassHeader
```

**Platform Dependencies in this path**:
- `VirtualFile` - core platform abstraction for file system
- `JvmDependenciesIndex` - uses `VirtualFile` for directory traversal
- `KotlinBinaryClassCache` - uses `ApplicationManager.getApplication().getService()`
- `VirtualFile.contentsToByteArray()` - file I/O through platform

### 2.2 Path: FQN → JavaClassFinder → Binary Java Class (without @Metadata)

When `JvmClassFileBasedSymbolProvider` finds a `.class` file without Kotlin metadata:

```
1. extractClassMetadata(classId: ClassId)
   │
   └─► kotlinClassFinder.findKotlinClassOrContent(classId)
       │
       └─► Returns ClassFileContent (not KotlinClass) if no @Metadata
           │
           └─► javaFacade.findClass(classId, knownContent)
               │
               └─► classFinder.findClass(Request(classId, knownContent))
                   │
                   ├─► [PSI path - current] KotlinJavaPsiFacade.findClass()
                   │   │
                   │   └─► KotlinCliJavaFileManager.findClass()
                   │       │
                   │       └─► JvmDependenciesIndex.findClasses()
                   │           │
                   │           └─► BinaryJavaClass(virtualFile, ...)
                   │               │
                   │               └─► ClassReader(virtualFile.contentsToByteArray())
                   │
                   └─► [java-direct path] JavaClassFinderOverAstImpl.findClass()
                       │
                       └─► Only handles SOURCES, returns null for binaries!
```

**Critical Issue**: `JavaClassFinderOverAstImpl` only indexes and parses Java *source* files. It has no mechanism to handle binary `.class` files from JARs.

### 2.3 Path: Source Session Java Class Resolution

When compiling Kotlin that references a Java source file:

```
1. JavaSymbolProvider.getClassLikeSymbolByClassId(classId)
   │
   └─► classCache.getValue(classId) { ... }
       │
       └─► javaFacade.findClass(classId)
           │
           └─► classFinder.findClass(Request(classId))
               │
               ├─► [PSI path] → parses .java file via PSI
               │
               └─► [java-direct path] JavaClassFinderOverAstImpl
                   │
                   └─► Parses .java file via KMP Java Parser
                       │
                       └─► Returns JavaClassOverAst
```

This path works correctly with `java-direct` for Java sources in the current module.

---

## 3. Platform Dependencies Map

### 3.1 Core Platform Types Used

| Type | Package | Usage |
|------|---------|-------|
| `VirtualFile` | `com.intellij.openapi.vfs` | File system abstraction everywhere |
| `VirtualFileSystem` | `com.intellij.openapi.vfs` | File system provider |
| `GlobalSearchScope` | `com.intellij.psi.search` | Scope filtering for searches |
| `Project` | `com.intellij.openapi.project` | Project container |
| `ApplicationManager` | `com.intellij.openapi.application` | Service registry |
| `PsiManager` | `com.intellij.psi` | PSI tree management |
| `PsiClass` | `com.intellij.psi` | Java class PSI representation |

### 3.2 Key Platform-Dependent Classes

| Class | Location | Platform Dependency |
|-------|----------|---------------------|
| `KotlinJavaPsiFacade` | `compiler/frontend.common.jvm/` | PSI, VirtualFile, caches |
| `KotlinCliJavaFileManagerImpl` | `compiler/cli/cli-base/` | CoreJavaFileManager, PSI |
| `CliVirtualFileFinder` | `compiler/cli/cli-base/` | VirtualFile, JvmDependenciesIndex |
| `VirtualFileKotlinClass` | `compiler/frontend.common.jvm/` | VirtualFile I/O |
| `KotlinBinaryClassCache` | `compiler/frontend.common.jvm/` | ApplicationManager service |
| `JvmDependenciesIndexImpl` | `compiler/cli/cli-base/` | VirtualFile for directory ops |
| `BinaryJavaClass` | `compiler/frontend.common.jvm/` | VirtualFile for content |

### 3.3 Services and Registrations

```kotlin
// In KotlinBinaryClassCache
ApplicationManager.getApplication().getService(KotlinBinaryClassCache::class.java)

// In VfsBasedProjectEnvironment
VirtualFileFinderFactory.getInstance(project)  // Project service

// In KotlinJavaPsiFacade
project.getService(KotlinJavaPsiFacade::class.java)
project.getService(JavaFileManager::class.java)
```

---

## 4. The Root Problem for java-direct

### 4.1 Current State

The `java-direct` module successfully:
- Parses Java source files without PSI
- Creates `JavaClass` implementations (`JavaClassOverAst`) over the AST
- Integrates with `FirJavaFacade` for converting Java → FIR

But it **fails** when:
1. The Java source references external types (from JDK, libraries)
2. The Kotlin source references types from binary dependencies
3. Any resolution requires looking up classes in JAR files

### 4.2 Why External Types Fail

When `JavaClassOverAst` is created, its supertype references (e.g., `extends Object`) need resolution. The `FirJavaFacade` converts this to FIR, which triggers:

```
FirJavaFacade.convertJavaClassToFir()
    → supertypes are JavaClassifierTypeOverAst
        → classifier property needs to resolve "Object" to a JavaClass
            → If local scope doesn't have it, must query external
                → External lookup needs KotlinClassFinder/JavaClassFinder
                    → These use VirtualFile infrastructure
                        → FAIL: java-direct has no access to this
```

### 4.3 The Architectural Gap

```
┌─────────────────────────────────────────────────────────────────┐
│                      What java-direct replaces                   │
│                                                                  │
│    JavaClassFinderOverAstImpl  ─────────► Java Sources (.java)  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   What java-direct DOESN'T replace               │
│                                                                  │
│    KotlinClassFinder / VirtualFileFinder  ──► Binary classes    │
│                                                (.class in JARs)  │
│                                                                  │
│    JvmDependenciesIndex  ──────────────────► Classpath indexing │
│                                                                  │
│    BinaryJavaClass  ────────────────────────► Java binaries     │
│                                                (without @Metadata)│
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. Possible Solutions

### 5.1 Short-term: Hybrid Approach (Recommended for Initial Implementation)

Keep platform dependencies for binary class resolution, only replace Java source parsing:

```kotlin
class JavaClassFinderOverAstImpl(
    private val sourceRoots: List<Path>,
    private val binaryClassFinder: JavaClassFinder?  // Delegate for binaries
) : JavaClassFinder {
    
    override fun findClass(request: Request): JavaClass? {
        // 1. Try source lookup first
        val fromSource = findInSources(request.classId)
        if (fromSource != null) return fromSource
        
        // 2. Fall back to binary lookup (platform-dependent)
        return binaryClassFinder?.findClass(request)
    }
}
```

**Pros**: Quick to implement, maintains compatibility
**Cons**: Still has platform dependency for binaries

### 5.2 Medium-term: Platform-Free Binary Reader

Create a new `BinaryKotlinClassFinder` that:
1. Uses standard Java NIO for file I/O (no VirtualFile)
2. Uses ASM directly for reading `.class` files
3. Implements its own classpath indexing

```kotlin
interface ClasspathIndex {
    fun findClassFile(classId: ClassId): Path?
    fun listPackage(packageFqName: FqName): Set<String>
}

class NioBasedKotlinClassFinder(
    private val classpathIndex: ClasspathIndex
) : KotlinClassFinder {
    
    override fun findKotlinClassOrContent(classId: ClassId, metadataVersion: MetadataVersion): Result? {
        val path = classpathIndex.findClassFile(classId) ?: return null
        val bytes = Files.readAllBytes(path)
        return createKotlinClassOrContent(bytes, metadataVersion)
    }
}
```

**Pros**: Fully platform-independent
**Cons**: More work, needs new indexing implementation

### 5.3 Long-term: Unified Resolution Layer

Create a unified abstraction that handles both sources and binaries:

```kotlin
interface UnifiedClassFinder {
    fun findClass(classId: ClassId): ClassRepresentation?
    
    sealed class ClassRepresentation {
        class FromSource(val javaClass: JavaClass) : ClassRepresentation()
        class FromBinary(val binaryClass: KotlinJvmBinaryClass) : ClassRepresentation()
        class FromBinaryJava(val bytes: ByteArray) : ClassRepresentation()
    }
}
```

---

## 6. Specific Changes Needed

### 6.1 For Short-term Hybrid Approach

1. **Modify `VfsBasedProjectEnvironmentOverAst`**:
   ```kotlin
   override fun getFirJavaFacade(...): FirJavaFacadeForSource {
       val sourcesFinder = JavaClassFinderOverAstImpl(javaSourceRoots)
       val binaryFinder = project.createJavaClassFinder(librariesScope.asPsiSearchScope(), null)
       val combinedFinder = CombinedJavaClassFinder(sourcesFinder, binaryFinder)
       return FirJavaFacadeForSource(firSession, baseModuleData, combinedFinder)
   }
   ```

2. **Create `CombinedJavaClassFinder`**:
   ```kotlin
   class CombinedJavaClassFinder(
       private val sourcesFinder: JavaClassFinder,
       private val binaryFinder: JavaClassFinder
   ) : JavaClassFinder {
       override fun findClass(request: Request): JavaClass? =
           sourcesFinder.findClass(request) ?: binaryFinder.findClass(request)
       
       override fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean): JavaPackage? =
           // Combine packages from both sources
   }
   ```

3. **Ensure library session uses standard providers**:
   - Library session should continue using `JvmClassFileBasedSymbolProvider`
   - Only source session uses the custom Java facade

### 6.2 For Medium-term Platform-Free Approach

1. **Create `NioClasspathIndex`** - index JARs and directories without VirtualFile
2. **Create `NioBinaryJavaClass`** - parse binary Java classes without VirtualFile
3. **Create `NioKotlinBinaryClass`** - read Kotlin metadata without VirtualFile
4. **Create `NioBasedVirtualFileFinder`** - implement `KotlinClassFinder` without platform

---

## 7. Testing Strategy

### 7.1 Test Case: `testGenericSamProjectedOut`

This test involves:
- Java source files (`Hello.java`, `SomeJavaClass.java`) with generics
- Kotlin source (`main.kt`) using SAM conversion
- References to `java.lang.String` (from JDK)

**Why it's a good test**:
- Exercises Java source parsing (java-direct handles)
- Exercises external type resolution (`String` from JDK)
- Exercises generic type parameters
- Exercises SAM conversion (needs proper Java method signatures)

### 7.2 Debugging Approach

Add exception-based tracing in key locations:

```kotlin
// In JavaClassFinderOverAstImpl.findClass()
if (classId.packageFqName.asString().startsWith("java.")) {
    throw RuntimeException("JavaClassFinderOverAstImpl queried for JDK class: $classId\n" +
        "Stack trace follows...")
}

// In CliVirtualFileFinder.findBinaryOrSigClass()
// Add logging to understand when binary lookup is invoked
```

### 7.3 Expected Fix Verification

After implementing the hybrid approach:
1. Java sources should parse via `JavaClassFinderOverAstImpl`
2. JDK classes should resolve via platform-based `JavaClassFinder`
3. Library classes should resolve via `JvmClassFileBasedSymbolProvider`
4. Test should pass without errors

---

## 8. Conclusions

### 8.1 Root Cause

The `java-direct` implementation is incomplete because it only handles Java source files, while the Kotlin compiler requires access to:
- Binary Kotlin classes (stdlib, libraries) via `KotlinClassFinder`
- Binary Java classes (JDK, libraries) via `JavaClassFinder`

### 8.2 Immediate Action

Implement the **hybrid approach** (Section 5.1) to unblock development:
1. Keep platform dependencies for binary class resolution
2. Use `java-direct` only for Java source parsing
3. Combine both finders in a delegating implementation

### 8.3 Future Work

To fully remove platform dependencies:
1. Implement NIO-based classpath indexing
2. Implement NIO-based binary class reading
3. Replace `VirtualFile` with `Path` throughout
4. Remove `ApplicationManager` service dependencies

---

## Appendix A: Key File Locations

| Component | File Path |
|-----------|-----------|
| KotlinJavaPsiFacade | `compiler/frontend.common.jvm/src/org/jetbrains/kotlin/resolve/jvm/KotlinJavaPsiFacade.java` |
| CliVirtualFileFinder | `compiler/cli/cli-base/src/org/jetbrains/kotlin/cli/jvm/compiler/CliVirtualFileFinder.kt` |
| VirtualFileKotlinClass | `compiler/frontend.common.jvm/src/org/jetbrains/kotlin/load/kotlin/VirtualFileKotlinClass.kt` |
| KotlinBinaryClassCache | `compiler/frontend.common.jvm/src/org/jetbrains/kotlin/load/kotlin/KotlinBinaryClassCache.kt` |
| JvmDependenciesIndexImpl | `compiler/cli/cli-base/src/org/jetbrains/kotlin/cli/jvm/index/JvmDependenciesIndexImpl.kt` |
| BinaryJavaClass | `compiler/frontend.common.jvm/src/org/jetbrains/kotlin/load/java/structure/impl/classFiles/BinaryJavaClass.kt` |
| JvmClassFileBasedSymbolProvider | `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/deserialization/JvmClassFileBasedSymbolProvider.kt` |
| JavaSymbolProvider | `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/JavaSymbolProvider.kt` |
| FirJavaFacade | `compiler/fir/fir-jvm/src/org/jetbrains/kotlin/fir/java/FirJavaFacade.kt` |
| VfsBasedProjectEnvironment | `compiler/cli/src/org/jetbrains/kotlin/cli/jvm/compiler/VfsBasedProjectEnvironment.kt` |
| JavaClassFinderOverAstImpl | `compiler/java-direct/src/org/jetbrains/kotlin/java/direct/JavaClassFinderOverAstImpl.kt` |

## Appendix B: Call Graph for Type Resolution

```
Kotlin code: val x: String = ...
    │
    ↓
FirCallableSymbol lookup for "String"
    │
    ↓
FirSymbolProvider.getClassLikeSymbolByClassId(ClassId("kotlin", "String"))
    │
    ↓
[Library session] JvmClassFileBasedSymbolProvider.extractClassMetadata()
    │
    ↓
kotlinClassFinder.findKotlinClassOrContent()
    │
    ↓
CliVirtualFileFinder.findVirtualFileWithHeader()
    │
    ↓
JvmDependenciesIndex.findClasses() with VirtualFile operations
    │
    ↓
VirtualFileKotlinClass.create() → reads @Metadata
    │
    ↓
FirTypeDeserializer deserializes kotlin.String
```
