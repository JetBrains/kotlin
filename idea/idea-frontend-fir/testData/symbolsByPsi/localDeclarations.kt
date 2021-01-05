fun yyy() {
    val q = 10
    fun aaa() {}

    class F {}
}

// SYMBOLS:
/*
KtFirLocalVariableSymbol:
  annotatedType: [] kotlin/Int
  isVal: true
  name: q
  origin: SOURCE
  symbolKind: LOCAL

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotations: []
  callableIdIfNonLocal: null
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

KtFirClassOrObjectSymbol:
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
  primaryConstructor: KtFirConstructorSymbol(<constructor>)
  superTypes: [[] kotlin/Any]
  symbolKind: LOCAL
  typeParameters: []
  visibility: LOCAL

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotations: []
  callableIdIfNonLocal: yyy
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
