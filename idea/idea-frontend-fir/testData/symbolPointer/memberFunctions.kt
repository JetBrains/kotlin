class A {
    fun x(): Int
    fun y()
}

// SYMBOLS:
KtFirFunctionSymbol:
  callableIdIfNonLocal: A.x
  isExtension: false
  isOperator: false
  isSuspend: false
  modality: FINAL
  name: x
  origin: SOURCE
  receiverType: null
  symbolKind: MEMBER
  type: kotlin/Int
  typeParameters: []
  valueParameters: []

KtFirFunctionSymbol:
  callableIdIfNonLocal: A.y
  isExtension: false
  isOperator: false
  isSuspend: false
  modality: FINAL
  name: y
  origin: SOURCE
  receiverType: null
  symbolKind: MEMBER
  type: kotlin/Unit
  typeParameters: []
  valueParameters: []

KtFirClassOrObjectSymbol:
  classIdIfNonLocal: A
  classKind: CLASS
  modality: FINAL
  name: A
  origin: SOURCE
  symbolKind: TOP_LEVEL
  typeParameters: []
