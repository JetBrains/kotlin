fun <X> foo(x: X) {}

// RESULT
/*
KtFirTypeParameterSymbol:
  isReified: false
  name: X
  origin: SOURCE
  upperBounds: [kotlin/Any?]
  variance: INVARIANT

KtFirValueParameterSymbol:
  annotatedType: [] X
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  hasDefaultValue: false
  isVararg: false
  name: x
  origin: SOURCE
  symbolKind: LOCAL

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: /foo
  dispatchType: null
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: FINAL
  name: foo
  origin: SOURCE
  receiverType: null
  symbolKind: TOP_LEVEL
  typeParameters: [KtFirTypeParameterSymbol(X)]
  valueParameters: [KtFirValueParameterSymbol(x)]
  visibility: Public
*/
