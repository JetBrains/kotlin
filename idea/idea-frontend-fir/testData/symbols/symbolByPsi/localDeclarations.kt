fun yyy() {
    val q = 10
    fun aaa() {}

    class F {}
}

// RESULT
/*
KtFirLocalVariableSymbol:
  annotatedType: [] kotlin/Int
  isVal: true
  name: q
  origin: SOURCE
  symbolKind: LOCAL

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  dispatchType: null
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: FINAL
  name: aaa
  origin: SOURCE
  receiverType: null
  symbolKind: LOCAL
  typeParameters: []
  valueParameters: []
  visibility: LOCAL

KtFirNamedClassOrObjectSymbol:
  annotationClassIds: []
  annotations: []
  classIdIfNonLocal: null
  classKind: CLASS
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: false
  modality: FINAL
  name: F
  origin: SOURCE
  superTypes: [[] kotlin/Any]
  symbolKind: LOCAL
  typeParameters: []
  visibility: LOCAL

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: yyy
  dispatchType: null
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: FINAL
  name: yyy
  origin: SOURCE
  receiverType: null
  symbolKind: TOP_LEVEL
  typeParameters: []
  valueParameters: []
  visibility: PUBLIC
*/
