fun yyy() {
    val q = 10
    fun aaa() {}

    class F {}
}

// SYMBOLS:
/*
KtFirLocalVariableSymbol:
  isVal: true
  name: q
  origin: SOURCE
  symbolKind: LOCAL
  type: kotlin/Int

KtFirFunctionSymbol:
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
  type: kotlin/Unit
  typeParameters: []
  valueParameters: []
  visibility: LOCAL

KtFirClassOrObjectSymbol:
  annotations: []
  classIdIfNonLocal: null
  classKind: CLASS
  companionObject: null
  isInner: false
  modality: FINAL
  name: F
  origin: SOURCE
  primaryConstructor: KtFirConstructorSymbol(<constructor>)
  superTypes: [kotlin/Any]
  symbolKind: LOCAL
  typeParameters: []
  visibility: LOCAL

KtFirFunctionSymbol:
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
  type: kotlin/Unit
  typeParameters: []
  valueParameters: []
  visibility: PUBLIC
*/
