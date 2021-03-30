class A {
  val x: Int = 10
  val Int.y get() = this
}

// RESULT
/*
KtFirKotlinPropertySymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: /A.x
  dispatchType: A
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
  symbolKind: MEMBER
  visibility: Public

KtFirPropertyGetterSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  dispatchType: null
  hasBody: true
  isDefault: false
  isInline: false
  isOverride: false
  modality: FINAL
  origin: SOURCE
  symbolKind: TOP_LEVEL
  visibility: Public

KtFirKotlinPropertySymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: /A.y
  dispatchType: A
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
  symbolKind: MEMBER
  visibility: Public

KtFirNamedClassOrObjectSymbol:
  annotationClassIds: []
  annotations: []
  classIdIfNonLocal: A
  classKind: CLASS
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: false
  modality: FINAL
  name: A
  origin: SOURCE
  superTypes: [[] kotlin/Any]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: Public
*/
