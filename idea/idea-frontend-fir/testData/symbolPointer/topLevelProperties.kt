val x: Int = 10
val Int.y get() = this

// SYMBOLS:
KtFirPropertySymbol:
  annotations: []
  callableIdIfNonLocal: x
  getter: KtFirPropertyGetterSymbol(<getter>)
  hasBackingField: true
  isConst: false
  isExtension: false
  isOverride: false
  isVal: true
  modality: FINAL
  name: x
  origin: SOURCE
  receiverType: null
  setter: null
  symbolKind: TOP_LEVEL
  type: kotlin/Int
  visibility: PUBLIC

KtFirPropertyGetterSymbol:
  isDefault: false
  isInline: false
  isOverride: false
  modality: FINAL
  origin: SOURCE
  symbolKind: TOP_LEVEL
  type: kotlin/Int
  visibility: PUBLIC

KtFirPropertySymbol:
  annotations: []
  callableIdIfNonLocal: y
  getter: KtFirPropertyGetterSymbol(<getter>)
  hasBackingField: false
  isConst: false
  isExtension: true
  isOverride: false
  isVal: true
  modality: FINAL
  name: y
  origin: SOURCE
  receiverType: kotlin/Int
  setter: null
  symbolKind: TOP_LEVEL
  type: kotlin/Int
  visibility: PUBLIC
