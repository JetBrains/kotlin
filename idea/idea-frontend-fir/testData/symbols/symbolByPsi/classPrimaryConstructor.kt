class A() {
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
  hasStableParameterNames: true
  isExtension: false
  isPrimary: true
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
  superTypes: [
    [] kotlin/Any
  ]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: Public
*/
