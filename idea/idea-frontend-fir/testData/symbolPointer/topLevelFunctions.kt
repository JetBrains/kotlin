fun x(): Int = 10
fun y() {}

// SYMBOLS:
KtFirFunctionSymbol:
  callableIdIfNonLocal: x
  isExtension: false
  isOperator: false
  isSuspend: false
  modality: FINAL
  name: x
  origin: SOURCE
  receiverType: null
  symbolKind: TOP_LEVEL
  type: kotlin/Int
  typeParameters: []
  valueParameters: []

KtFirFunctionSymbol:
  callableIdIfNonLocal: y
  isExtension: false
  isOperator: false
  isSuspend: false
  modality: FINAL
  name: y
  origin: SOURCE
  receiverType: null
  symbolKind: TOP_LEVEL
  type: kotlin/Unit
  typeParameters: []
  valueParameters: []
