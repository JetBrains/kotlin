# Analysis API

> **Note: Analysis API is currently under active development and does not provide any compatibility guaranties.**

Analysis API is an API which allows analysing Kotlin code and retrieve compiler-related information. This includes:
expression types, symbols, smartcast, and many others. Analysis API is an API which is used to implement Kotlin Plugin 2.0 which is under
active development now.

Analysis API is Kotlin compiler implementation independent . That means that it do not expose any Kotlin compiler internals.

Analysis API itself is a just set of interfaces/abstract classes without specific implementation inside. But there are two implementations
currently available.

* FIR-based implementation. FIR is a new compiler frontend which is a part of K2 Kotlin compiler. Read more:
    * [Analysis API FIR Implementation documentation](analysis-api-fir.md)
    * [Analysis API FIR Implementation source code](../../../analysis/analysis-api-fir)
    * [FIR Compiler documentation](../../fir/fir-basics.md)
* FE1.0-based implementation. FE1.0 is an original Kotlin compiler frontend. Read more:
    * [Analysis API FE1.0 Implementation documentation](analysis-api-fe10.md)
    * [Analysis API FE1.0 Implementation source code](../../../analysis/analysis-api-fe10)

Analysis API can work outside IntelliJ IDEA (e.g, it can be used for implementing Kotlin LSP server) but it still uses some basic classes
form IntelliJ Core. This is needed mostly for working with [PsiElements](https://plugins.jetbrains.com/docs/intellij/psi-elements.html),
lexical and syntax analysis.

## Services registration

The Analysis API defines services, extension points, service implementations, and so on in XML files.
Such files are located in resources under `/META-INF/analysis-api/` directory.

The implementor of the Analysis API has to register such definitions on its side.

- For FE1.0-based implementation it is `/META-INF/analysis-api/analysis-api-fe10.xml` file.
- For FIR-based implementation it is `/META-INF/analysis-api/analysis-api-fir.xml` file.

## KaSession

`KaSession` is a view to the project modules and libraries from some fixed module, so called use-site module. From
analysis `KaSession` we can see only modules and libraries which are transitive dependencies of the use-site module.
`KaSession` contains set of functions to interact with **Analysis API** and it is the only way to interact with it.

`KaSession` has the following contracts:

* **Lifecycle Owners** created in **KaSession Scope** should not be leaked outside that scope
* `KaSession` receiver parameter should not be written to a variable or used as a function parameter. For decomposing analysis code
  which should happen in **KaSession Scope** consider passing `KaSession` as function receiver parameter.
* If function accept * **Lifecycle Owner** as parameter, it should always have `KaSession` extension
  receiver: `fun KaSession.doSmthWithSymbol(symbol: KaSymbol) {...}`

## KaSession Scope

All interaction with the Analysis API should be performed **only** in **KaSession Scope**. To enter such scope `analyse`
function should be used:

```kotlin
fun <R> analyse(contextElement: KtElement, action: KaSession.() -> R): R
```

Where `action` lambda represents the **KaSession Scope**.

## Lifecycle Owners

Every Lifecycle Owner has its lifecycle which is defined by corresponding `KaLifetimeToken`. There is a special
function `analyseWithCustomToken` which allows specifying needed behaviour. There are also analyse function which is made for the IDE which
analyses with `KaReadActionConfinementLifetimeToken`

`KaReadActionConfinementLifetimeToken` has the following contracts:

* Accessibility contracts
    * Analysis should not be called from **EDT Thread**
        * If you have no choice consider using `analyseInModalWindow` function instead (but it may be rather slow and also shows a modal
          window, so use it with caution)
    * Analysis should be called from a **read action**
    * Analysis should not be called outside **KaSession Scope** (i.e, outside `analyse(context) { ... }` lambda
* Validity contracts:
    * Lifecycle Owner is valid only inside Analysis Context it was created in.

## KaSymbol

`KaSymbol`is an **Lifecycle Owner** that describes Kotlin or Java (as Kotlin sees it) declaration. `KaSymbol`represents:

* Source Kotlin/Java declarations
* Decompiled Kotlin/Java declarations
* Synthetic Kotlin declarations (e.g, copy method of data class)

Consider you want to take a symbol you got in one **KaSession Scope** and use in another. It cannot be done directly as `KaSymbol`
can not be leaked outside **KaSession Scope**. But there is such thing as `
KaSymbolPointer`. For every `KaSymbol`corresponding `KaSymbolPointer` can be created in one analysis session and symbol can be restored by
using it in another. If between creating a symbol and restoring it, corresponding declaration changed then the symbol will not be restored
and `KaSymbolPointer.restore` call will return the null value.

## KaType

`KaType` is an **Lifecycle Owner** that represents a Kotlin type in compiler-independent way.

## KaScope

The **Lifecycle Owner** which can answer the following queries:

* What callable (functions and properties) symbols does this scope contains
* What classifier (classes, type aliases, and type parameters) symbols does this scope contains

For now the following scopes available:

* **KaMemberScope** — contains members of the class with all members from super types,
* **KaDeclaredMemberScope** — contains members of class which are directly declared in the class,
* **KaPackageMemberScope** — contains all top-level declarations contained in the package,
* **KaTypeScope** — contains all members which can be called on expression of some type. 

