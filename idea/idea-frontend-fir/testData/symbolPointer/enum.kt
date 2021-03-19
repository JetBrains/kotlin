enum class X {
  Y, Z;
}

// RESULT
/*
KtFirEnumEntrySymbol:
  annotatedType: [] X
  containingEnumClassIdIfNonLocal: X
  name: Y
  origin: SOURCE
  symbolKind: MEMBER

KtFirEnumEntrySymbol:
  annotatedType: [] X
  containingEnumClassIdIfNonLocal: X
  name: Z
  origin: SOURCE
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
  visibility: PUBLIC
*/
