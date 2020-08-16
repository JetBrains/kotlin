fun foo(x: Int) {}

// SYMBOLS:
/*
KtFirFunctionValueParameterSymbol:
  hasDefaultValue: false
  isVararg: false
  name: x
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER
  type: kotlin/Int

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
  typeParameters: []
  valueParameters: [KtFirFunctionValueParameterSymbol(x)]
*/
