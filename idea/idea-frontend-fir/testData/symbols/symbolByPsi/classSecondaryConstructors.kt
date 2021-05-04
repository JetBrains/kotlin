class A() {
  constructor(x: Int): this()
  constructor(y: Int, z: String) : this(y)
}

// RESULT
/*
KtFirConstructorSymbol:
  annotatedType: [] A
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: A
  dispatchType: null
  isExtension: false
  isPrimary: true
  origin: SOURCE
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
  visibility: Public

KtFirValueParameterSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  hasDefaultValue: false
  isExtension: false
  isVararg: false
  name: x
  origin: SOURCE
  receiverType: null
  symbolKind: LOCAL

KtFirConstructorSymbol:
  annotatedType: [] A
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: A
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: SOURCE
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(x)]
  visibility: Public

KtFirValueParameterSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  hasDefaultValue: false
  isExtension: false
  isVararg: false
  name: y
  origin: SOURCE
  receiverType: null
  symbolKind: LOCAL

KtFirValueParameterSymbol:
  annotatedType: [] kotlin/String
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  hasDefaultValue: false
  isExtension: false
  isVararg: false
  name: z
  origin: SOURCE
  receiverType: null
  symbolKind: LOCAL

KtFirConstructorSymbol:
  annotatedType: [] A
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: A
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: SOURCE
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(y), KtFirValueParameterSymbol(z)]
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
