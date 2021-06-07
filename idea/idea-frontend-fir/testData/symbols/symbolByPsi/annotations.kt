annotation class Anno(val param1: String, val param2: Int)

@Anno(param1 = "param", 2)
class X {
    @Anno("funparam", 3)
    fun x() {

    }
}

// RESULT
/*
KtFirValueParameterSymbol:
  annotatedType: [] kotlin/String
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  hasDefaultValue: false
  isExtension: false
  isVararg: false
  name: param1
  origin: SOURCE
  receiverType: null
  symbolKind: LOCAL

KtFirValueParameterSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  hasDefaultValue: false
  isExtension: false
  isVararg: false
  name: param2
  origin: SOURCE
  receiverType: null
  symbolKind: LOCAL

KtFirConstructorSymbol:
  annotatedType: [] Anno
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: Anno
  dispatchType: null
  isExtension: false
  isPrimary: true
  origin: SOURCE
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(param1), KtFirValueParameterSymbol(param2)]
  visibility: Public

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
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotationClassIds: [Anno]
  annotations: [Anno(param1 = funparam, param2 = 3)]
  callableIdIfNonLocal: /X.x
  dispatchType: X
  isExtension: false
  isExternal: false
  isInfix: false
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
  visibility: Public

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
  visibility: Public
*/
