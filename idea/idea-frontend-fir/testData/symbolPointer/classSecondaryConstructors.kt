class A() {
    constructor(x: Int): this()
    constructor(y: Int, z: String) : this(y)
}

// SYMBOLS:
KtFirConstructorSymbol:
  containingClassIdIfNonLocal: A
  isPrimary: true
  origin: SOURCE
  symbolKind: MEMBER
  type: A
  valueParameters: []

KtFirFunctionValueParameterSymbol:
  hasDefaultValue: false
  isVararg: false
  name: x
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER
  type: kotlin/Int

KtFirConstructorSymbol:
  containingClassIdIfNonLocal: A
  isPrimary: false
  origin: SOURCE
  symbolKind: MEMBER
  type: A
  valueParameters: Could not render due to java.lang.ClassCastException: org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirFunctionValueParameterSymbol cannot be cast to org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirConstructorValueParameterSymbol

KtFirFunctionValueParameterSymbol:
  hasDefaultValue: false
  isVararg: false
  name: y
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER
  type: kotlin/Int

KtFirFunctionValueParameterSymbol:
  hasDefaultValue: false
  isVararg: false
  name: z
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER
  type: kotlin/String

KtFirConstructorSymbol:
  containingClassIdIfNonLocal: A
  isPrimary: false
  origin: SOURCE
  symbolKind: MEMBER
  type: A
  valueParameters: Could not render due to java.lang.ClassCastException: org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirFunctionValueParameterSymbol cannot be cast to org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirConstructorValueParameterSymbol

KtFirClassOrObjectSymbol:
  classIdIfNonLocal: A
  classKind: CLASS
  modality: FINAL
  name: A
  origin: SOURCE
  symbolKind: TOP_LEVEL
  typeParameters: []
