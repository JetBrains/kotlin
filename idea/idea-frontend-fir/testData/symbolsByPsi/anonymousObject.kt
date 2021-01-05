class AnonymousContainer {
    val anonymousObject = object : Runnable {
        override fun run() {

        }
        val data = 123
    }
}

// SYMBOLS:
/*
KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotations: []
  callableIdIfNonLocal: <anonymous>.run
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: true
  isSuspend: false
  modality: FINAL
  name: run
  origin: SOURCE
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
  visibility: PUBLIC

KtFirKotlinPropertySymbol:
  annotatedType: [] kotlin/Int
  annotations: []
  callableIdIfNonLocal: <anonymous>.data
  getter: KtFirPropertyGetterSymbol(<getter>)
  hasBackingField: true
  hasGetter: true
  hasSetter: false
  initializer: 123
  isConst: false
  isExtension: false
  isLateInit: false
  isOverride: false
  isVal: true
  modality: FINAL
  name: data
  origin: SOURCE
  receiverType: null
  setter: null
  symbolKind: MEMBER
  visibility: PUBLIC

KtFirAnonymousObjectSymbol:
  annotations: []
  origin: SOURCE
  superTypes: [[] java/lang/Runnable]
  symbolKind: LOCAL

KtFirKotlinPropertySymbol:
  annotatedType: [] java/lang/Runnable
  annotations: []
  callableIdIfNonLocal: AnonymousContainer.anonymousObject
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
  name: anonymousObject
  origin: SOURCE
  receiverType: null
  setter: null
  symbolKind: MEMBER
  visibility: PUBLIC

KtFirClassOrObjectSymbol:
  annotations: []
  classIdIfNonLocal: AnonymousContainer
  classKind: CLASS
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: false
  modality: FINAL
  name: AnonymousContainer
  origin: SOURCE
  primaryConstructor: KtFirConstructorSymbol(<constructor>)
  superTypes: [[] kotlin/Any]
  symbolKind: TOP_LEVEL
  typeParameters: []
  visibility: PUBLIC
*/
