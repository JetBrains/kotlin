val x: Int = 10
val Int.y get() = this

// SYMBOLS:
KtFirKotlinPropertySymbol:
  annotatedType: [] kotlin/Int
  annotations: []
  callableIdIfNonLocal: x
  getter: KtFirPropertyGetterSymbol(<getter>)
  hasBackingField: true
  hasGetter: true
  hasSetter: false
  initializer: 10
  isConst: false
  isExtension: false
  isLateInit: false
  isOverride: false
  isVal: true
  modality: FINAL
  name: x
  origin: SOURCE
  receiverType: null
  setter: null
  symbolKind: TOP_LEVEL
  visibility: PUBLIC

KtFirPropertyGetterSymbol:
  annotatedType: [] kotlin/Int
  annotations: []
  hasBody: true
  isDefault: false
  isInline: false
  isOverride: false
  modality: FINAL
  origin: SOURCE
  symbolKind: TOP_LEVEL
  visibility: PUBLIC

KtFirKotlinPropertySymbol:
  annotatedType: [] kotlin/Int
  annotations: []
  callableIdIfNonLocal: y
  getter: KtFirPropertyGetterSymbol(<getter>)
  hasBackingField: false
  hasGetter: true
  hasSetter: false
  initializer: null
  isConst: false
  isExtension: true
  isLateInit: false
  isOverride: false
  isVal: true
  modality: FINAL
  name: y
  origin: SOURCE
  receiverType: [] kotlin/Int
  setter: null
  symbolKind: TOP_LEVEL
  visibility: PUBLIC
