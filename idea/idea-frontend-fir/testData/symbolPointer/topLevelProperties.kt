val x: Int = 10
val Int.y get() = this

// SYMBOLS:
KtFirPropertySymbol:
  callableIdIfNonLocal: x
  isExtension: false
  isVal: true
  modality: FINAL
  name: x
  origin: SOURCE
  receiverType: null
  symbolKind: TOP_LEVEL
  type: kotlin/Int

KtFirPropertyGetterSymbol:
  isDefault: false
  modality: FINAL
  origin: SOURCE
  type: kotlin/Int

KtFirPropertySymbol:
  callableIdIfNonLocal: y
  isExtension: true
  isVal: true
  modality: FINAL
  name: y
  origin: SOURCE
  receiverType: kotlin/Int
  symbolKind: TOP_LEVEL
  type: kotlin/Int
