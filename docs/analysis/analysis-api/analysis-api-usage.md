# Analysis API Usage

To use Analysis API, you need to be in the `KtAnalysisSessoin` context. Basically, it means that you need to call [`analyse(contextElement: KtElement, action: KtAnalysisSession.() -> R): R`](https://github.com/JetBrains/kotlin/blob/master/analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api/KtAnalysisSessionProvider.kt#L106) function. All your actions with Analysis API will be performed with `KtAnalysisSessoin` receiver available.
```kotlin
analyse(psiElementForContext) { // you are inside KtAnalysisSession Context
    // you can use a wide variety of Analysis API functions here: work with types, symbols, signatures, scopes and more.
}
```
## Functions
You may want to decompose your logic into functions. In such a case, add KtAnalysisSession receiver to it. Such a thing should be done even if your function does not use the receiver. The approach may look cumbersome, but it is important to be sure that we do not publish resolution results. Publishing resolution results may cause memory leaks or working with out-of-date resolution results.
```kotlin
analyse(psiElementForContext) {
    val symbol = getSymbol()
    ...
}

fun KtAnalysisSession.getSymbol() : KtSymbol {
    // the body of this function is also inside KtAnalysisSession context and may use it 
}
```

## No leakages of KtLifetimeTokenOwners from KtAnalysisSession context
All `KtLifetimeTokenOwners` you get inside a `KtAnalysisSessoin` context should never leak it. But you may:
* Store your `KtLifetimeTokenOwners` as a field inside a class that implements `KtLifetimeTokenOwners`. This way your outer class is `KtLifetimeTokenOwners` itself and all rules apply to it. 
* Pass it to another function with a `KtAnalysisSessoin` receiver.