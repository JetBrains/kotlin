var x: Int = 0
    get() = <caret>field
    set(value) {
        field = value
    }

// RESULT
/*
KtFirBackingFieldSymbol:
  annotatedType: [] kotlin/Int
  callableIdIfNonLocal: null
  isExtension: false
  name: field
  origin: PROPERTY_BACKING_FIELD
  owningProperty: KtFirKotlinPropertySymbol(x)
  receiverType: null
  symbolKind: LOCAL
*/
