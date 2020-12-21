class A {
  fun x(): Int
  fun y()
}

// SYMBOLS:
KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotations: []
  callableIdIfNonLocal: A.x
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
  visibility: PUBLIC

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotations: []
  callableIdIfNonLocal: A.y
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: FINAL
  name: y
  origin: SOURCE
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
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
  superTypes: [[] kotlin/Any]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC
