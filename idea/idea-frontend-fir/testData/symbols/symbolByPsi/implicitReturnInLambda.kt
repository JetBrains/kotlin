fun foo() {
    val lam1 = { a: Int ->
        val b = 1
        a + b
    }

    val lam2 = { a: Int ->
        val c = 1
        if (a > 0)
            a + c
        else
            a - c
    }

    bar {
        if (it > 5) return
        val b = 1
        it + b
    }
}

private inline fun bar(lmbd: (Int) -> Int) {
    lmbd(1)
}

// RESULT
/*
KtFirValueParameterSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  hasDefaultValue: false
  isExtension: false
  isVararg: false
  name: a
  origin: SOURCE
  receiverType: null
  symbolKind: LOCAL

KtFirLocalVariableSymbol:
  annotatedType: [] kotlin/Int
  callableIdIfNonLocal: null
  isExtension: false
  isVal: true
  name: b
  origin: SOURCE
  receiverType: null
  symbolKind: LOCAL

KtFirAnonymousFunctionSymbol:
  annotatedType: [] kotlin/Int
  callableIdIfNonLocal: null
  isExtension: false
  origin: SOURCE
  receiverType: null
  symbolKind: LOCAL
  valueParameters: [KtFirValueParameterSymbol(a)]

KtFirLocalVariableSymbol:
  annotatedType: [] kotlin/Function1<kotlin/Int, kotlin/Int>
  callableIdIfNonLocal: null
  isExtension: false
  isVal: true
  name: lam1
  origin: SOURCE
  receiverType: null
  symbolKind: LOCAL

KtFirValueParameterSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  hasDefaultValue: false
  isExtension: false
  isVararg: false
  name: a
  origin: SOURCE
  receiverType: null
  symbolKind: LOCAL

KtFirLocalVariableSymbol:
  annotatedType: [] kotlin/Int
  callableIdIfNonLocal: null
  isExtension: false
  isVal: true
  name: c
  origin: SOURCE
  receiverType: null
  symbolKind: LOCAL

KtFirAnonymousFunctionSymbol:
  annotatedType: [] kotlin/Int
  callableIdIfNonLocal: null
  isExtension: false
  origin: SOURCE
  receiverType: null
  symbolKind: LOCAL
  valueParameters: [KtFirValueParameterSymbol(a)]

KtFirLocalVariableSymbol:
  annotatedType: [] kotlin/Function1<kotlin/Int, kotlin/Int>
  callableIdIfNonLocal: null
  isExtension: false
  isVal: true
  name: lam2
  origin: SOURCE
  receiverType: null
  symbolKind: LOCAL

KtFirLocalVariableSymbol:
  annotatedType: [] kotlin/Int
  callableIdIfNonLocal: null
  isExtension: false
  isVal: true
  name: b
  origin: SOURCE
  receiverType: null
  symbolKind: LOCAL

KtFirAnonymousFunctionSymbol:
  annotatedType: [] kotlin/Int
  callableIdIfNonLocal: null
  isExtension: false
  origin: SOURCE
  receiverType: null
  symbolKind: LOCAL
  valueParameters: [KtFirValueParameterSymbol(it)]

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: /foo
  dispatchType: null
  isExtension: false
  isExternal: false
  isInfix: false
  isInline: false
  isOperator: false
  isOverride: false
  isStatic: false
  isSuspend: false
  modality: FINAL
  name: foo
  origin: SOURCE
  receiverType: null
  symbolKind: TOP_LEVEL
  typeParameters: []
  valueParameters: []
  visibility: Public

KtFirValueParameterSymbol:
  annotatedType: [] kotlin/Function1<kotlin/Int, kotlin/Int>
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  hasDefaultValue: false
  isExtension: false
  isVararg: false
  name: lmbd
  origin: SOURCE
  receiverType: null
  symbolKind: LOCAL

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: /bar
  dispatchType: null
  isExtension: false
  isExternal: false
  isInfix: false
  isInline: true
  isOperator: false
  isOverride: false
  isStatic: false
  isSuspend: false
  modality: FINAL
  name: bar
  origin: SOURCE
  receiverType: null
  symbolKind: TOP_LEVEL
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(lmbd)]
  visibility: Private
*/
