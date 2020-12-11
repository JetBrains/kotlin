class A {
  fun x(): Int
  fun y()
}

// SYMBOLS:
KtFirFunctionSymbol:
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
  receiverTypeAndAnnotations: null
  symbolKind: MEMBER
  type: kotlin/Int
  typeParameters: []
  valueParameters: []
  visibility: PUBLIC

KtFirFunctionSymbol:
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
  receiverTypeAndAnnotations: null
  symbolKind: MEMBER
  type: kotlin/Unit
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
  superTypes: [kotlin/Any]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC
