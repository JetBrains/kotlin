fun <X> foo(x: X) {}

// SYMBOLS:
/*
KtFirTypeParameterSymbol:
  name: X
  origin: SOURCE

KtFirFunctionValueParameterSymbol:
  hasDefaultValue: false
  isVararg: false
  name: x
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER
  type: X

KtFirFunctionSymbol:
  callableIdIfNonLocal: foo
  isExtension: false
  isOperator: false
  isSuspend: false
  modality: FINAL
  name: foo
  origin: SOURCE
  receiverType: null
  symbolKind: TOP_LEVEL
  type: kotlin/Unit
  typeParameters: [KtFirTypeParameterSymbol(X)]
  valueParameters: [KtFirFunctionValueParameterSymbol(x)]
*/
