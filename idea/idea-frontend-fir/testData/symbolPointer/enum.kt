enum class X {
  Y, Z;
}

// SYMBOLS:
KtFirEnumEntrySymbol:
  containingEnumClassIdIfNonLocal: X
  name: Y
  origin: SOURCE
  symbolKind: MEMBER
  type: X

KtFirEnumEntrySymbol:
  containingEnumClassIdIfNonLocal: X
  name: Z
  origin: SOURCE
  symbolKind: MEMBER
  type: X

KtFirClassOrObjectSymbol:
  annotations: []
  classIdIfNonLocal: X
  classKind: ENUM_CLASS
  companionObject: null
  isInner: false
  modality: FINAL
  name: X
  origin: SOURCE
  primaryConstructor: KtFirConstructorSymbol(<constructor>)
  superTypes: [kotlin/Enum<X>]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC
