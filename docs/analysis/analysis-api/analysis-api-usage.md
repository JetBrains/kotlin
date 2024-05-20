# Analysis API Usage

To use Analysis API, you need to be in the `KaSession` context. Basically, it means that you need to call [`analyse(contextElement: KtElement, action: KaSession.() -> R): R`](https://github.com/JetBrains/kotlin/blob/master/analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api/KaAnalysisSessionProvider.kt#L106) function. All your actions with Analysis API will be performed with `KaSession` receiver available.
```kotlin
analyse(psiElementForContext) { // you are inside KaSession Context
    // you can use a wide variety of Analysis API functions here: work with types, symbols, signatures, scopes and more.
}
```
## Functions
You may want to decompose your logic into functions. In such a case, add KaSession receiver to it. Such a thing should be done even if your function does not use the receiver. The approach may look cumbersome, but it is important to be sure that we do not publish resolution results. Publishing resolution results may cause memory leaks or working with out-of-date resolution results.
```kotlin
analyse(psiElementForContext) {
    val symbol = getSymbol()
    ...
}

fun KaSession.getSymbol() : KaSymbol {
    // the body of this function is also inside KaSession context and may use it 
}
```

## No leakages of KaLifetimeTokenOwners from KaSession context
All `KaLifetimeTokenOwners` you get inside a `KaSession` context should never leak it. But you may:
* Store your `KaLifetimeTokenOwners` as a field inside a class that implements `KaLifetimeTokenOwners`. This way your outer class is `KaLifetimeTokenOwners` itself and all rules apply to it. 
* Pass it to another function with a `KaSession` receiver.