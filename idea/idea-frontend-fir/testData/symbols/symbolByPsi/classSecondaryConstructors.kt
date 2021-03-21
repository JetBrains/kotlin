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
  containingClassIdIfNonLocal: A
  dispatchType: null
  isPrimary: true
  origin: SOURCE
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
  visibility: PUBLIC

KtFirValueParameterSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  hasDefaultValue: false
  isVararg: false
  name: x
  origin: SOURCE
  symbolKind: LOCAL

KtFirConstructorSymbol:
  annotatedType: [] A
  annotationClassIds: []
  annotations: []
  containingClassIdIfNonLocal: A
  dispatchType: null
  isPrimary: false
  origin: SOURCE
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(x)]
  visibility: PUBLIC

KtFirValueParameterSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  hasDefaultValue: false
  isVararg: false
  name: y
  origin: SOURCE
  symbolKind: LOCAL

KtFirValueParameterSymbol:
  annotatedType: [] kotlin/String
  annotationClassIds: []
  annotations: []
  hasDefaultValue: false
  isVararg: false
  name: z
  origin: SOURCE
  symbolKind: LOCAL

KtFirConstructorSymbol:
  annotatedType: [] A
  annotationClassIds: []
  annotations: []
  containingClassIdIfNonLocal: A
  dispatchType: null
  isPrimary: false
  origin: SOURCE
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(y), KtFirValueParameterSymbol(z)]
  visibility: PUBLIC

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
  visibility: PUBLIC
*/
