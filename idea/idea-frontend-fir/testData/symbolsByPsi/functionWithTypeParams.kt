fun <X> foo(x: X) {}

// SYMBOLS:
/*
KtFirTypeParameterSymbol:
  name: X
  origin: SOURCE
  upperBounds: [kotlin/Any?]

KtFirFunctionValueParameterSymbol:
  annotatedType: [] X
  annotations: []
  hasDefaultValue: false
  isVararg: false
  name: x
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotations: []
  callableIdIfNonLocal: foo
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
  valueParameters: [KtFirFunctionValueParameterSymbol(x)]
  visibility: PUBLIC
*/
