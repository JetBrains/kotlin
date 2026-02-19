# Light Classes

## What are Light Classes?

The compiler reuses the IntelliJ IDEA [Java PSI](https://github.com/JetBrains/intellij-community/tree/902e84fce4b9d969603502b3c3e8698125c50ce8/java/java-psi-api/src/com/intellij/psi) ([Program Structure Interface](https://plugins.jetbrains.com/docs/intellij/psi.html)) to analyze Java code (e.g., to resolve a Java method's return type).
To perform such analysis, the [Java resolver](https://github.com/JetBrains/intellij-community/tree/902e84fce4b9d969603502b3c3e8698125c50ce8/java/java-psi-impl/src/com/intellij/psi/impl/source/resolve) under the hood needs to understand Kotlin code if it is referenced from Java.
However, Kotlin PSI is not compatible with Java PSI, so some bridging is required to allow the Java resolver to work with Kotlin PSI.

Thus, Light Classes are **read-only** synthetic Java PSI representations of Kotlin declarations.

Simple examples: 
- [KtClass](https://github.com/JetBrains/kotlin/blob/0aeb8ceb73abffa73480065a91c377388c7bb6b9/compiler/psi/psi-api/src/org/jetbrains/kotlin/psi/KtClass.kt#L16) might be represented as a [PsiClass](https://github.com/JetBrains/intellij-community/blob/5d190eaae73e51c1dec185890f2301ef9c540070/java/java-psi-api/src/com/intellij/psi/PsiClass.java#L26)
- [KtNamedFunction](https://github.com/JetBrains/kotlin/blob/0aeb8ceb73abffa73480065a91c377388c7bb6b9/compiler/psi/psi-api/src/org/jetbrains/kotlin/psi/KtNamedFunction.java#L27) might be represented as a [PsiMethod](https://github.com/JetBrains/intellij-community/blob/5d190eaae73e51c1dec185890f2301ef9c540070/java/java-psi-api/src/com/intellij/psi/PsiMethod.java#L24) (potentially, as a few `PsiMethod`s)
- [KtProperty](https://github.com/JetBrains/kotlin/blob/0aeb8ceb73abffa73480065a91c377388c7bb6b9/compiler/psi/psi-api/src/org/jetbrains/kotlin/psi/KtProperty.java#L31)'s property accessors might be represented as [PsiMethod](https://github.com/JetBrains/intellij-community/blob/5d190eaae73e51c1dec185890f2301ef9c540070/java/java-psi-api/src/com/intellij/psi/PsiMethod.java#L24)s, and the backing field as a [PsiField](https://github.com/JetBrains/intellij-community/blob/5d190eaae73e51c1dec185890f2301ef9c540070/java/java-psi-api/src/com/intellij/psi/PsiField.java#L14)
- [KtAnnotationEntry](https://github.com/JetBrains/kotlin/blob/0aeb8ceb73abffa73480065a91c377388c7bb6b9/compiler/psi/psi-api/src/org/jetbrains/kotlin/psi/KtAnnotationEntry.java#L23) might be represented as a [PsiAnnotation](https://github.com/JetBrains/intellij-community/blob/5d190eaae73e51c1dec185890f2301ef9c540070/java/java-psi-api/src/com/intellij/psi/PsiAnnotation.java#L18) (potentially, as a few `PsiAnnotation`s)

In most cases, light classes mirror the Kotlin JVM bytecode of the corresponding Kotlin declarations, so the bytecode should be treated as the source of truth.

There is no strict one-to-one relationship between Kotlin PSI and their counterparts in Java PSI.
For instance, even if a Kotlin class doesn't declare any constructors, a default constructor will still be generated at the JVM bytecode level, and it will be callable from Java.
Similarly, in the `PsiClass` light class, there will be a `PsiMethod` for it, but that method will lack a link to the source `KtPrimaryConstructor` (as it isn't present in the source code).

### Name

The name "Light Classes" comes from the initial implementation, inherited from [AbstractLightClass](https://github.com/JetBrains/intellij-community/blob/902e84fce4b9d969603502b3c3e8698125c50ce8/java/java-psi-impl/src/com/intellij/psi/impl/light/AbstractLightClass.java#L22).  
Light classes in IntelliJ IDEA represent PSI not backed by any real files.

Also, they are "light" in the sense that they don't have to implement all APIs of Java PSI.

### What are Light Classes for?

Light classes are essential for Java resolver interoperability. They allow Java code to analyze references to Kotlin code.

In particular, they provide support for:
1. **Java Highlighting**: Java code can seamlessly call and reference Kotlin code in the editor
2. **Nested Kotlin resolution**: There might be cross-references between Kotlin and Java code, using different code analyzers for each language.

    Example:
    ```kotlin
    // FILE: KotlinUsage.kt
    class KotlinUsage: JavaClass() {
      fun foo() {
        bar()
      }
    }
    ```
    ```java
    // FILE: JavaClass.java
    public class JavaClass extends KotlinBaseClass {
    }
    ```
    ```kotlin
    // FILE: KotlinBaseClass.kt
    open class KotlinBaseClass {
      fun bar() {}
    }
    ```
    To resolve `bar()` in `foo()`, the Kotlin resolver would ask `JavaClass` about its hierarchy. To answer that, the Java resolver would need to resolve `KotlinBaseClass`, which cannot be done without light classes.

### What are Light Classes *not* for?

1. **Modifications**: Unlike `PsiElement`s created for source declarations, light classes provide a read-only view of Kotlin declarations. Calling mutating methods on them will result in an exception
2. **Kotlin Code Analysis**: Light classes are not intended for Kotlin code analysis, as they only provide resolution-required information. For instance, declarations that are not visible from Java code might not be represented.
   Anti-examples:
   - Code insight for Kotlin code
   - [UAST](https://plugins.jetbrains.com/docs/intellij/uast.html)

## Entry Points

1. [LightClassUtilsKt](https://github.com/JetBrains/kotlin/blob/e8516744ee31633d8ac3a0a4b24510f3b9482fff/analysis/light-classes-base/src/org/jetbrains/kotlin/asJava/lightClassUtils.kt) – a set of PSI utilities to get light classes
2. [KotlinAsJavaSupport](https://github.com/JetBrains/kotlin/blob/5298abf2d68907701d391ac9f9d3f05ecc527b96/analysis/light-classes-base/src/org/jetbrains/kotlin/asJava/KotlinAsJavaSupport.kt#L19) – a service that provides light classes. Usually not used directly, but rather via utilities
3. [JavaElementFinder](https://github.com/JetBrains/kotlin/blob/1708b4fe4885a72fe1518b3a3b862cfb83e5dd4a/analysis/light-classes-base/src/org/jetbrains/kotlin/asJava/finder/JavaElementFinder.kt#L29) – the main entry point for Java resolve. It uses `KotlinAsJavaSupport` to find light classes by FQN
4. *(TBD [KT-78862](https://youtrack.jetbrains.com/issue/KT-78862))* [KaSymbol](https://github.com/JetBrains/kotlin/blob/b14aa74069d60d86107109dc0d0eca634aa43b0e/analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api/symbols/KaSymbol.kt#L28) -> `PsiElement?` utilities

## Implementations

Some common sense guidelines:
- Light classes should be as lightweight as possible to avoid affecting performance (both CPU and memory)
- Prefer not to store information directly in light classes unless necessary (e.g., a name computation can be done lazily and stored in a hard reference if it is on a hot path). It is usually a trade-off between performance and memory usage
- Potentially heavy computations should be performed lazily

### Symbol Light Classes (a.k.a. SLC)

The latest implementation of light classes is powered by the Analysis API. Currently, it is used only for sources ([KT-77787](https://youtrack.jetbrains.com/issue/KT-77787)).

The main benefit of using the Analysis API is that compiler plugins are supported out of the box by SLC.

A limitation is that SLC must adhere to the resolution contracts of the Kotlin compiler. This means that some scenarios are sensitive to the amount of resolution work performed. In the worst case, this can cause [contract violations](https://github.com/JetBrains/kotlin/blob/9d0caf4833bd2bcc836261a7b7553c63f76a7feb/compiler/fir/tree/src/org/jetbrains/kotlin/fir/symbols/FirLazyDeclarationResolver.kt#L95).

Example: [8afeffee](https://github.com/JetBrains/kotlin/commit/8afeffee487fadcf3860c0f9e1090e9072dad55a).

The most sensitive place is class members creation (e.g., [SymbolLightClassForClassOrObject#getOwnMethods](https://github.com/JetBrains/kotlin/blob/fca89107685c41a935315409c545e4776c639387/analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/classes/SymbolLightClassForClassOrObject.kt#L118)) as it is heavily used in the Java resolver.

The implementation is registered in [SymbolKotlinAsJavaSupport](./src/org/jetbrains/kotlin/light/classes/symbol/SymbolKotlinAsJavaSupport.kt).

### Decompiled Light Classes (a.k.a. DLC)

The implementation of light classes which uses `.class` Kotlin output to build Java stubs and provide Java PSI mapping "out of the box".
The implementation is straightforward but has some limitations due to its simplicity. Mainly, it doesn't support constructs that are stored in the bytecode differently than they would be in Java.
For instance, type annotations are [not supported](https://youtrack.jetbrains.com/issue/KT-77329/External-Kotlin-library-with-Nls-annotation-on-type-yields-warnings-when-using-it-in-localization-context#focus=Comments-27-12059527.0-0).

The entry point is [DecompiledLightClassesFactory](https://github.com/JetBrains/kotlin/blob/c9bffea9fab1805e3a6d6a535637264a6ee0281e/analysis/decompiled/light-classes-for-decompiled/src/org/jetbrains/kotlin/analysis/decompiled/light/classes/DecompiledLightClassesFactory.kt#L29)

The next evolution step: [KT-77787](https://youtrack.jetbrains.com/issue/KT-77787) Replace DLC with SLC

### Ultra Light Classes (a.k.a. ULC)

The K1 implementation of light classes which is built on top of Kotlin PSI.

Location: [compiler/light-classes](https://github.com/JetBrains/kotlin/tree/f5596b29eebb1a1e45df9db96957952e4cd69d2f/compiler/light-classes)
