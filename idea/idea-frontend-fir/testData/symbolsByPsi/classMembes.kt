class A {
    val a: Int = 10
    fun x() = 10
}

// SYMBOLS:
/*
KtFirPropertySymbol:
  callableIdIfNonLocal: A.a
  isExtension: false
  isVal: true
  modality: FINAL
  name: a
  origin: SOURCE
  receiverType: kotlin/Int
  symbolKind: MEMBER
  type: kotlin/Int

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

KtFirClassOrObjectSymbol:
  classIdIfNonLocal: A
  classKind: CLASS
  modality: FINAL
  name: A
  origin: SOURCE
  symbolKind: TOP_LEVEL
  typeParameters: []
*/
