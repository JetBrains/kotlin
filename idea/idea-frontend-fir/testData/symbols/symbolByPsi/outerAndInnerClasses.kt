class X<T> {
    inner class Y<T1>
    class Z<T2>

    fun <T3> foo() {
        class U<T4> {
            inner class K<T5>
            class C<T6>
        }
    }
}

// RESULT
/*
KtFirTypeParameterSymbol:
  isReified: false
  name: T
  origin: SOURCE
  upperBounds: [kotlin/Any?]
  variance: INVARIANT

KtFirTypeParameterSymbol:
  isReified: false
  name: T1
  origin: SOURCE
  upperBounds: [kotlin/Any?]
  variance: INVARIANT

KtFirNamedClassOrObjectSymbol:
  annotationClassIds: []
  annotations: []
  classIdIfNonLocal: X.Y
  classKind: CLASS
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: true
  modality: FINAL
  name: Y
  origin: SOURCE
  superTypes: [[] kotlin/Any]
  symbolKind: MEMBER
  typeParameters: [KtFirTypeParameterSymbol(T1)]
  visibility: Public

KtFirTypeParameterSymbol:
  isReified: false
  name: T2
  origin: SOURCE
  upperBounds: [kotlin/Any?]
  variance: INVARIANT

KtFirNamedClassOrObjectSymbol:
  annotationClassIds: []
  annotations: []
  classIdIfNonLocal: X.Z
  classKind: CLASS
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: false
  modality: FINAL
  name: Z
  origin: SOURCE
  superTypes: [[] kotlin/Any]
  symbolKind: MEMBER
  typeParameters: [KtFirTypeParameterSymbol(T2)]
  visibility: Public

KtFirTypeParameterSymbol:
  isReified: false
  name: T3
  origin: SOURCE
  upperBounds: [kotlin/Any?]
  variance: INVARIANT

KtFirTypeParameterSymbol:
  isReified: false
  name: T4
  origin: SOURCE
  upperBounds: [kotlin/Any?]
  variance: INVARIANT

KtFirTypeParameterSymbol:
  isReified: false
  name: T5
  origin: SOURCE
  upperBounds: [kotlin/Any?]
  variance: INVARIANT

KtFirNamedClassOrObjectSymbol:
  annotationClassIds: []
  annotations: []
  classIdIfNonLocal: null
  classKind: CLASS
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: true
  modality: FINAL
  name: K
  origin: SOURCE
  superTypes: [[] kotlin/Any]
  symbolKind: LOCAL
  typeParameters: [KtFirTypeParameterSymbol(T5)]
  visibility: Local

KtFirTypeParameterSymbol:
  isReified: false
  name: T6
  origin: SOURCE
  upperBounds: [kotlin/Any?]
  variance: INVARIANT

KtFirNamedClassOrObjectSymbol:
  annotationClassIds: []
  annotations: []
  classIdIfNonLocal: null
  classKind: CLASS
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: false
  modality: FINAL
  name: C
  origin: SOURCE
  superTypes: [[] kotlin/Any]
  symbolKind: LOCAL
  typeParameters: [KtFirTypeParameterSymbol(T6)]
  visibility: Local

KtFirNamedClassOrObjectSymbol:
  annotationClassIds: []
  annotations: []
  classIdIfNonLocal: null
  classKind: CLASS
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: false
  modality: FINAL
  name: U
  origin: SOURCE
  superTypes: [[] kotlin/Any]
  symbolKind: LOCAL
  typeParameters: [KtFirTypeParameterSymbol(T4)]
  visibility: Local

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: /X.foo
  dispatchType: X<T>
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: FINAL
  name: foo
  origin: SOURCE
  receiverType: null
  symbolKind: MEMBER
  typeParameters: [KtFirTypeParameterSymbol(T3)]
  valueParameters: []
  visibility: Public

KtFirNamedClassOrObjectSymbol:
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
  superTypes: [[] kotlin/Any]
  symbolKind: TOP_LEVEL
  typeParameters: [KtFirTypeParameterSymbol(T)]
  visibility: Public
*/
