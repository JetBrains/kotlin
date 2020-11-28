
annotation class Anno(val param1: String, val param2: Int)

@Anno(param1 = "param", 2)
class X {
    @Anno("funparam", 3)
    fun x() {

    }
}

// SYMBOLS:
/*
KtFirFunctionValueParameterSymbol:
  annotations: []
  hasDefaultValue: false
  isVararg: false
  name: param1
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER
  type: kotlin/String

KtFirFunctionValueParameterSymbol:
  annotations: []
  hasDefaultValue: false
  isVararg: false
  name: param2
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER
  type: kotlin/Int

KtFirConstructorSymbol:
  annotations: []
  containingClassIdIfNonLocal: Anno
  isPrimary: true
  origin: SOURCE
  symbolKind: MEMBER
  type: Anno
  valueParameters: Could not render due to java.lang.ClassCastException: org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirFunctionValueParameterSymbol cannot be cast to org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirConstructorValueParameterSymbol
  visibility: PUBLIC

KtFirClassOrObjectSymbol:
  annotations: []
  classIdIfNonLocal: Anno
  classKind: ANNOTATION_CLASS
  companionObject: null
  isInner: false
  modality: FINAL
  name: Anno
  origin: SOURCE
  primaryConstructor: KtFirConstructorSymbol(<constructor>)
  superTypes: [kotlin/Annotation]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC

KtFirFunctionSymbol:
  annotations: [Anno(param1 = funparam, param2 = 3)]
  callableIdIfNonLocal: X.x
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: FINAL
  name: x
  origin: SOURCE
  receiverType: null
  symbolKind: MEMBER
  type: kotlin/Unit
  typeParameters: []
  valueParameters: []
  visibility: PUBLIC

KtFirClassOrObjectSymbol:
  annotations: [Anno(param1 = param, param2 = 2)]
  classIdIfNonLocal: X
  classKind: CLASS
  companionObject: null
  isInner: false
  modality: FINAL
  name: X
  origin: SOURCE
  primaryConstructor: KtFirConstructorSymbol(<constructor>)
  superTypes: [kotlin/Any]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC
*/
