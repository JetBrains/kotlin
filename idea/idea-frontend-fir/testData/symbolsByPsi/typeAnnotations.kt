
@Target(AnnotationTarget.TYPE)
annotation class Anno1
@Target(AnnotationTarget.TYPE)
annotation class Anno2
@Target(AnnotationTarget.TYPE)
annotation class Anno3
@Target(AnnotationTarget.TYPE)
annotation class Anno4

interface I

class X : @Anno1 I {
    fun f(arg: @Anno2 I): @Anno3 I = arg
    val x: @Anno4 I = this
}

// SYMBOLS:
/*
KtFirClassOrObjectSymbol:
  annotationClassIds: [kotlin/annotation/Target]
  annotations: [kotlin/annotation/Target(allowedTargets = KtUnsupportedConstantValue)]
  classIdIfNonLocal: Anno1
  classKind: ANNOTATION_CLASS
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: false
  modality: FINAL
  name: Anno1
  origin: SOURCE
  superTypes: [[] kotlin/Annotation]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC

KtFirClassOrObjectSymbol:
  annotationClassIds: [kotlin/annotation/Target]
  annotations: [kotlin/annotation/Target(allowedTargets = KtUnsupportedConstantValue)]
  classIdIfNonLocal: Anno2
  classKind: ANNOTATION_CLASS
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: false
  modality: FINAL
  name: Anno2
  origin: SOURCE
  superTypes: [[] kotlin/Annotation]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC

KtFirClassOrObjectSymbol:
  annotationClassIds: [kotlin/annotation/Target]
  annotations: [kotlin/annotation/Target(allowedTargets = KtUnsupportedConstantValue)]
  classIdIfNonLocal: Anno3
  classKind: ANNOTATION_CLASS
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: false
  modality: FINAL
  name: Anno3
  origin: SOURCE
  superTypes: [[] kotlin/Annotation]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC

KtFirClassOrObjectSymbol:
  annotationClassIds: [kotlin/annotation/Target]
  annotations: [kotlin/annotation/Target(allowedTargets = KtUnsupportedConstantValue)]
  classIdIfNonLocal: Anno4
  classKind: ANNOTATION_CLASS
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: false
  modality: FINAL
  name: Anno4
  origin: SOURCE
  superTypes: [[] kotlin/Annotation]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC

KtFirClassOrObjectSymbol:
  annotationClassIds: []
  annotations: []
  classIdIfNonLocal: I
  classKind: INTERFACE
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: false
  modality: ABSTRACT
  name: I
  origin: SOURCE
  superTypes: [[] kotlin/Any]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC

KtFirFunctionValueParameterSymbol:
  annotatedType: [Anno2()] I
  annotationClassIds: []
  annotations: []
  hasDefaultValue: false
  isVararg: false
  name: arg
  origin: SOURCE
  symbolKind: NON_PROPERTY_PARAMETER

KtFirFunctionSymbol:
  annotatedType: [Anno3()] I
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: X.f
  dispatchType: X
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: FINAL
  name: f
  origin: SOURCE
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirFunctionValueParameterSymbol(arg)]
  visibility: PUBLIC

KtFirKotlinPropertySymbol:
  annotatedType: [Anno4()] I
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: X.x
  dispatchType: X
  getter: KtFirPropertyGetterSymbol(<getter>)
  hasBackingField: true
  hasGetter: true
  hasSetter: false
  initializer: KtUnsupportedConstantValue
  isConst: false
  isExtension: false
  isLateInit: false
  isOverride: false
  isVal: true
  modality: FINAL
  name: x
  origin: SOURCE
  receiverType: null
  setter: null
  symbolKind: MEMBER
  visibility: PUBLIC

KtFirClassOrObjectSymbol:
  annotationClassIds: []
  annotations: []
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
  superTypes: [[Anno1()] I]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC
*/
