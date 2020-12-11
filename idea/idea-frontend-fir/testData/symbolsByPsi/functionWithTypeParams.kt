fun <X> foo(x: X) {}

// SYMBOLS:
/*
KtFirTypeParameterSymbol:
  name: X
  origin: SOURCE
  upperBounds: [kotlin/Any?]

KtFirFunctionValueParameterSymbol:
  annotations: []
  hasDefaultValue: false
  isVararg: false
  name: x
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER
  type: X

KtFirFunctionSymbol:
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
  receiverTypeAndAnnotations: null
  symbolKind: TOP_LEVEL
  type: kotlin/Unit
  typeParameters: [KtFirTypeParameterSymbol(X)]
  valueParameters: [KtFirFunctionValueParameterSymbol(x)]
  visibility: PUBLIC
*/
