class A() {
  constructor(x: Int): this()
  constructor(y: Int, z: String) : this(y)
}

// SYMBOLS:
KtFirConstructorSymbol:
  annotatedType: [] A
  annotations: []
  containingClassIdIfNonLocal: A
  dispatchType: null
  isPrimary: true
  origin: SOURCE
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
  visibility: PUBLIC

KtFirFunctionValueParameterSymbol:
  annotatedType: [] kotlin/Int
  annotations: []
  hasDefaultValue: false
  isVararg: false
  name: x
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER

KtFirConstructorSymbol:
  annotatedType: [] A
  annotations: []
  containingClassIdIfNonLocal: A
  dispatchType: null
  isPrimary: false
  origin: SOURCE
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: Could not render due to java.lang.ClassCastException: org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirFunctionValueParameterSymbol cannot be cast to org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirConstructorValueParameterSymbol
  visibility: PUBLIC

KtFirFunctionValueParameterSymbol:
  annotatedType: [] kotlin/Int
  annotations: []
  hasDefaultValue: false
  isVararg: false
  name: y
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER

KtFirFunctionValueParameterSymbol:
  annotatedType: [] kotlin/String
  annotations: []
  hasDefaultValue: false
  isVararg: false
  name: z
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER

KtFirConstructorSymbol:
  annotatedType: [] A
  annotations: []
  containingClassIdIfNonLocal: A
  dispatchType: null
  isPrimary: false
  origin: SOURCE
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: Could not render due to java.lang.ClassCastException: org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirFunctionValueParameterSymbol cannot be cast to org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirConstructorValueParameterSymbol
  visibility: PUBLIC

KtFirClassOrObjectSymbol:
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
