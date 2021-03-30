class A {
    val a: Int = 10
    fun x() = 10
}

// RESULT
/*
KtFirKotlinPropertySymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: /A.a
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
  name: a
  origin: SOURCE
  receiverType: null
  setter: null
  symbolKind: MEMBER
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: /A.x
  dispatchType: A
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: FINAL
  name: x
  origin: SOURCE
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
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
