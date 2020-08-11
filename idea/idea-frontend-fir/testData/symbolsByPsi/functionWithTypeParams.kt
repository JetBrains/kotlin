fun <X> foo(x: X) {}

// SYMBOLS:
/*
KtFirTypeParameterSymbol:
  name: X
  origin: SOURCE

KtFirFunctionValueParameterSymbol:
  isVararg: false
  name: x
  origin: SOURCE
  symbolKind: LOCAL
  type: X

KtFirFunctionSymbol:
  fqName: foo
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
