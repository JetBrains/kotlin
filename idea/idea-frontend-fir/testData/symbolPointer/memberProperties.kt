class A {
  val x: Int = 10
  val Int.y get() = this
}

// SYMBOLS:
KtFirPropertySymbol:
  annotations: []
  callableIdIfNonLocal: A.x
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
  symbolKind: MEMBER
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
  callableIdIfNonLocal: A.y
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
  symbolKind: MEMBER
  type: kotlin/Int
  visibility: PUBLIC

KtFirClassOrObjectSymbol:
  annotations: []
  classIdIfNonLocal: A
  classKind: CLASS
  companionObject: null
  isInner: false
  modality: FINAL
  name: A
  origin: SOURCE
  primaryConstructor: KtFirConstructorSymbol(<constructor>)
  superTypes: [kotlin/Any]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC
