
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
  annotatedType: [] kotlin/String
  annotationClassIds: []
  annotations: []
  hasDefaultValue: false
  isVararg: false
  name: param1
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER

KtFirFunctionValueParameterSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  hasDefaultValue: false
  isVararg: false
  name: param2
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER

KtFirConstructorSymbol:
  annotatedType: [] Anno
  annotationClassIds: []
  annotations: []
  containingClassIdIfNonLocal: Anno
  dispatchType: null
  isPrimary: true
  origin: SOURCE
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: Could not render due to java.lang.ClassCastException: org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirFunctionValueParameterSymbol cannot be cast to org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirConstructorValueParameterSymbol
  visibility: PUBLIC

KtFirNamedClassOrObjectSymbol:
  annotationClassIds: []
  annotations: []
  classIdIfNonLocal: Anno
  classKind: ANNOTATION_CLASS
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: false
  modality: FINAL
  name: Anno
  origin: SOURCE
  superTypes: [[] kotlin/Annotation]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotationClassIds: [Anno]
  annotations: [Anno(param1 = funparam, param2 = 3)]
  callableIdIfNonLocal: X.x
  dispatchType: X
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
  typeParameters: []
  valueParameters: []
  visibility: PUBLIC

KtFirNamedClassOrObjectSymbol:
  annotationClassIds: [Anno]
  annotations: [Anno(param1 = param, param2 = 2)]
  classIdIfNonLocal: X
  classKind: CLASS
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: false
  modality: FINAL
  name: X
  origin: SOURCE
  superTypes: [[] kotlin/Any]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC
*/
