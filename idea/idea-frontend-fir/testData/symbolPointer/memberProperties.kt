class A {
    val x: Int = 10
    val Int.y = this
}

// SYMBOLS:
KtFirPropertySymbol:
  fqName: A.x
  isExtension: false
  isVal: true
  modality: FINAL
  name: x
  origin: SOURCE
  receiverType: kotlin/Int
  symbolKind: MEMBER
  type: kotlin/Int

KtFirPropertySymbol:
  fqName: A.y
  isExtension: true
  isVal: true
  modality: FINAL
  name: y
  origin: SOURCE
  receiverType: A
  symbolKind: MEMBER
  type: A

KtFirClassOrObjectSymbol:
  classId: A
  classKind: CLASS
  modality: FINAL
  name: A
  origin: SOURCE
  symbolKind: TOP_LEVEL
  typeParameters: []
