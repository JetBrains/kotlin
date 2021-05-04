enum class X {
  Y, Z;
}

// RESULT
/*
KtFirEnumEntrySymbol:
  annotatedType: [] X
  callableIdIfNonLocal: /X.Y
  containingEnumClassIdIfNonLocal: X
  isExtension: false
  name: Y
  origin: SOURCE
  receiverType: null
  symbolKind: MEMBER

KtFirEnumEntrySymbol:
  annotatedType: [] X
  callableIdIfNonLocal: /X.Z
  containingEnumClassIdIfNonLocal: X
  isExtension: false
  name: Z
  origin: SOURCE
  receiverType: null
  symbolKind: MEMBER

KtFirNamedClassOrObjectSymbol:
  annotationClassIds: []
  annotations: []
  classIdIfNonLocal: X
  classKind: ENUM_CLASS
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: false
  modality: FINAL
  name: X
  origin: SOURCE
  superTypes: [[] kotlin/Enum<X>]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: Public
*/
