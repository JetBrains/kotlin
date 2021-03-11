// WITH_RUNTIME

fun x() {
    val a = <caret>ArrayList(listOf(1))
}
// RESULT

/*
KtFirConstructorSymbol:
  annotatedType: [] java/util/ArrayList<E>
  annotationClassIds: []
  annotations: []
  containingClassIdIfNonLocal: java/util/ArrayList
  dispatchType: java/util/ArrayList<E>
  isPrimary: false
  origin: JAVA
  symbolKind: MEMBER
  typeParameters: [KtFirTypeParameterSymbol(E)]
  valueParameters: [KtFirConstructorValueParameterSymbol(c)]
  visibility: PUBLIC
*/

