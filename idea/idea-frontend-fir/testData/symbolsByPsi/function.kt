fun foo(x: Int) {}

// SYMBOLS:
/*
KtFirFunctionValueParameterSymbol:
  annotations: []
  hasDefaultValue: false
  isVararg: false
  name: x
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER
  type: kotlin/Int

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
  receiverType: null
  symbolKind: TOP_LEVEL
  type: kotlin/Unit
  typeParameters: []
  valueParameters: [KtFirFunctionValueParameterSymbol(x)]
  visibility: PUBLIC
*/
