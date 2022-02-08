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

## KtAnalysisSession

`KtAnalysisSession` is a view to the project modules and libraries from some fixed module, so called use-site module. From
analysis `KtAnalysisSession` we can see only modules and libraries which are transitive dependencies of the use-site module.
`KtAnalysisSession` contains set of functions to interact with **Analysis API** and it is the only way to interact with it.

`KtAnalysisSession` has the following contracts:

* **Lifecycle Owners** created in **KtAnalysisSession Scope** should not be leaked outside that scope
* `KtAnalysisSession` receiver parameter should not be written to a variable or used as a function parameter. For decomposing analysis code
  which should happen in **KtAnalysisSession Scope** consider passing `KtAnalysisSession` as function receiver parameter.
* If function accept * **Lifecycle Owner** as parameter, it should always have `KtAnalysisSession` extension
  receiver: `fun KtAnalysisSession.doSmthWithSymbol(symbol: KtSymbol) {...}`

## KtAnalysisSession Scope

All interaction with the Analysis API should be performed **only** in **KtAnalysisSession Scope**. To enter such scope `analyse`
function should be used:

```kotlin
fun <R> analyze(contextElement: KtElement, action: KtAnalysisSession.() -> R): R
```

Where `action` lambda represents the **KtAnalysisSession Scope**.

## Lifecycle Owners

Every Lifecycle Owner has its lifecycle which is defined by corresponding `ValidityToken`. There is a special
function `analyseWithCustomToken` which allows specifying needed behaviour. There are also analyse function which is made for the IDE which
analyses with `ReadActionConfinementValidityToken`

`ReadActionConfinementValidityToken` has the following contracts:

* Accessibility contracts
    * Analysis should not be called from **EDT Thread**
        * If you have no choice consider using `analyseInModalWindow` function instead (but it may be rather slow and also shows a modal
          window, so use it with caution)
    * Analysis should be called from a **read action**
    * Analysis should not be called outside **KtAnalysisSession Scope** (i.e, outside `analyse(context) { ... }` lambda
* Validity contracts:
    * Lifecycle Owner is valid only inside Analysis Context it was created in.

## KtSymbol

`KtSymbol`is an **Lifecycle Owner** that describes Kotlin or Java (as Kotlin sees it) declaration. `KtSymbol`represents:

* Source Kotlin/Java declarations
* Decompiled Kotlin/Java declarations
* Synthetic Kotlin declarations (e.g, copy method of data class)

Consider you want to take a symbol you got in one **KtAnalysisSession Scope** and use in another. It cannot be done directly as `KtSymbol`
can not be leaked outside **KtAnalysisSession Scope**. But there is such thing as `
KtSymbolPointer`. For every `KtSymbol`corresponding `KtSymbolPointer` can be created in one analysis session and symbol can be restored by
using it in another. If between creating a symbol and restoring it, corresponding declaration changed then the symbol will not be restored
and `KtSymbolPointer.restore` call will return the null value.

## KtType

`KtType` is an **Lifecycle Owner** that represents a Kotlin type in compiler-independent way.

## KtScope

The **Lifecycle Owner** which can answer the following queries:

* What callable (functions and properties) symbols does this scope contains
* What classifier (classes, type aliases, and type parameters) symbols does this scope contains

For now the following scopes available:

* **KtMemberScope** — contains members of the class with all members from super types,
* **KtDeclaredMemberScope** — contains members of class which are directly declared in the class,
* **KtPackageMemberScope** — contains all top-level declarations contained in the package,
* **KtTypeScope** — contains all members which can be called on expression of some type. 

