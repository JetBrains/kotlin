fun x(): Int = 10
fun y() {}

// RESULT
/*
KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: /x
  dispatchType: null
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
  symbolKind: TOP_LEVEL
  typeParameters: []
  valueParameters: []
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: /y
  dispatchType: null
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: FINAL
  name: y
  origin: SOURCE
  receiverType: null
  symbolKind: TOP_LEVEL
  typeParameters: []
  valueParameters: []
  visibility: Public
*/
