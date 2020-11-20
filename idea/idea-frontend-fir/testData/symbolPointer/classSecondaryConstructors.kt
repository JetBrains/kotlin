class A() {
  constructor(x: Int): this()
  constructor(y: Int, z: String) : this(y)
}

// SYMBOLS:
KtFirConstructorSymbol:
  annotations: []
  containingClassIdIfNonLocal: A
  isPrimary: true
  origin: SOURCE
  symbolKind: MEMBER
  type: A
  valueParameters: []
  visibility: PUBLIC

KtFirFunctionValueParameterSymbol:
  annotations: []
  hasDefaultValue: false
  isVararg: false
  name: x
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER
  type: kotlin/Int

KtFirConstructorSymbol:
  annotations: []
  containingClassIdIfNonLocal: A
  isPrimary: false
  origin: SOURCE
  symbolKind: MEMBER
  type: A
  valueParameters: Could not render due to java.lang.ClassCastException: org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirFunctionValueParameterSymbol cannot be cast to org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirConstructorValueParameterSymbol
  visibility: PUBLIC

KtFirFunctionValueParameterSymbol:
  annotations: []
  hasDefaultValue: false
  isVararg: false
  name: y
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER
  type: kotlin/Int

KtFirFunctionValueParameterSymbol:
  annotations: []
  hasDefaultValue: false
  isVararg: false
  name: z
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER
  type: kotlin/String

KtFirConstructorSymbol:
  annotations: []
  containingClassIdIfNonLocal: A
  isPrimary: false
  origin: SOURCE
  symbolKind: MEMBER
  type: A
  valueParameters: Could not render due to java.lang.ClassCastException: org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirFunctionValueParameterSymbol cannot be cast to org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirConstructorValueParameterSymbol
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
