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
  classIdIfNonLocal: X
  classKind: ENUM_CLASS
  modality: FINAL
  name: X
  origin: SOURCE
  symbolKind: TOP_LEVEL
  typeParameters: []
