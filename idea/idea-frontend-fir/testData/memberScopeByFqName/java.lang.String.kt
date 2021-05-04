// DO_NOT_CHECK_SYMBOL_RESTORE

// class: java/lang/String

// RESULT
/*
KtFirJavaFieldSymbol:
  annotatedType: [] ft<kotlin/CharArray, kotlin/CharArray?>
  callableIdIfNonLocal: java/lang/String.value
  isExtension: false
  isVal: true
  modality: FINAL
  name: value
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  visibility: Private

KtFirJavaFieldSymbol:
  annotatedType: [] kotlin/Int
  callableIdIfNonLocal: java/lang/String.hash
  isExtension: false
  isVal: false
  modality: OPEN
  name: hash
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  visibility: Private

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.hash32
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: hash32
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
  visibility: PackageVisibility

KtFirJavaFieldSymbol:
  annotatedType: [] kotlin/Int
  callableIdIfNonLocal: java/lang/String.hash32
  isExtension: false
  isVal: false
  modality: OPEN
  name: hash32
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  visibility: Private

KtFirSyntheticJavaPropertySymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.length
  dispatchType: java/lang/String
  getter: KtFirPropertyGetterSymbol(<getter>)
  hasBackingField: true
  hasGetter: true
  hasSetter: false
  initializer: null
  isExtension: false
  isOverride: false
  isVal: true
  javaGetterName: length
  javaSetterName: null
  modality: OPEN
  name: length
  origin: JAVA_SYNTHETIC_PROPERTY
  receiverType: null
  setter: null
  symbolKind: MEMBER
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Boolean
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.isEmpty
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: isEmpty
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Char
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.charAt
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: charAt
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(index)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.codePointAt
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: codePointAt
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(index)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.codePointBefore
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: codePointBefore
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(index)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.codePointCount
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: codePointCount
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(beginIndex), KtFirValueParameterSymbol(endIndex)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.offsetByCodePoints
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: offsetByCodePoints
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(index), KtFirValueParameterSymbol(codePointOffset)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.getChars
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: getChars
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(dst), KtFirValueParameterSymbol(dstBegin)]
  visibility: PackageVisibility

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.getChars
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: getChars
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(srcBegin), KtFirValueParameterSymbol(srcEnd), KtFirValueParameterSymbol(dst), KtFirValueParameterSymbol(dstBegin)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Unit
  annotationClassIds: [kotlin/Deprecated]
  annotations: [kotlin/Deprecated(message = Deprecated in Java)]
  callableIdIfNonLocal: java/lang/String.getBytes
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: getBytes
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(srcBegin), KtFirValueParameterSymbol(srcEnd), KtFirValueParameterSymbol(dst), KtFirValueParameterSymbol(dstBegin)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/ByteArray
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.getBytes
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: getBytes
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(charsetName)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/ByteArray
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.getBytes
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: getBytes
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(charset)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/ByteArray
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.getBytes
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: getBytes
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Boolean
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.equals
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: true
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: equals
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(anObject)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Boolean
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.contentEquals
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: contentEquals
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(sb)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Boolean
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.contentEquals
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: contentEquals
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(cs)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Boolean
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.equalsIgnoreCase
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: equalsIgnoreCase
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(anotherString)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.compareTo
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: true
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: compareTo
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(anotherString)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.compareToIgnoreCase
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: compareToIgnoreCase
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(str)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Boolean
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.regionMatches
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: regionMatches
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(toffset), KtFirValueParameterSymbol(other), KtFirValueParameterSymbol(ooffset), KtFirValueParameterSymbol(len)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Boolean
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.regionMatches
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: regionMatches
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(ignoreCase), KtFirValueParameterSymbol(toffset), KtFirValueParameterSymbol(other), KtFirValueParameterSymbol(ooffset), KtFirValueParameterSymbol(len)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Boolean
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.startsWith
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: startsWith
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(prefix), KtFirValueParameterSymbol(toffset)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Boolean
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.startsWith
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: startsWith
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(prefix)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Boolean
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.endsWith
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: endsWith
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(suffix)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.hashCode
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: hashCode
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.indexOf
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: indexOf
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(ch)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.indexOf
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: indexOf
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(ch), KtFirValueParameterSymbol(fromIndex)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.indexOf
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: indexOf
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(str)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.indexOf
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: indexOf
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(str), KtFirValueParameterSymbol(fromIndex)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.indexOfSupplementary
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: indexOfSupplementary
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(ch), KtFirValueParameterSymbol(fromIndex)]
  visibility: Private

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.lastIndexOf
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: lastIndexOf
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(ch)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.lastIndexOf
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: lastIndexOf
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(ch), KtFirValueParameterSymbol(fromIndex)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.lastIndexOf
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: lastIndexOf
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(str)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.lastIndexOf
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: lastIndexOf
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(str), KtFirValueParameterSymbol(fromIndex)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Int
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.lastIndexOfSupplementary
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: lastIndexOfSupplementary
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(ch), KtFirValueParameterSymbol(fromIndex)]
  visibility: Private

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/String
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.substring
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: substring
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(beginIndex)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/String
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.substring
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: substring
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(beginIndex), KtFirValueParameterSymbol(endIndex)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/CharSequence
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.subSequence
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: subSequence
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(beginIndex), KtFirValueParameterSymbol(endIndex)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/String
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.concat
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: concat
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(str)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/String
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.replace
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: replace
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(oldChar), KtFirValueParameterSymbol(newChar)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/String
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.replace
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: replace
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(target), KtFirValueParameterSymbol(replacement)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Boolean
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.matches
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: matches
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(regex)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Boolean
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.contains
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: true
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: contains
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(s)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/String
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.replaceFirst
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: replaceFirst
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(regex), KtFirValueParameterSymbol(replacement)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/String
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.replaceAll
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: replaceAll
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(regex), KtFirValueParameterSymbol(replacement)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] ft<@EnhancedNullability kotlin/Array<ft<kotlin/String, kotlin/String?>>, @EnhancedNullability kotlin/Array<out ft<kotlin/String, kotlin/String?>>>
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.split
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: split
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(regex), KtFirValueParameterSymbol(limit)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] ft<@EnhancedNullability kotlin/Array<ft<kotlin/String, kotlin/String?>>, @EnhancedNullability kotlin/Array<out ft<kotlin/String, kotlin/String?>>>
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.split
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: split
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(regex)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/String
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.toLowerCase
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: toLowerCase
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(locale)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/String
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.toLowerCase
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: toLowerCase
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/String
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.toUpperCase
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: toUpperCase
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(locale)]
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/String
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.toUpperCase
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: toUpperCase
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/String
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.trim
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: trim
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/String
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: java/lang/String.toString
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: toString
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/CharArray
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.toCharArray
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: toCharArray
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] @EnhancedNullability kotlin/String
  annotationClassIds: [org/jetbrains/annotations/NotNull]
  annotations: [org/jetbrains/annotations/NotNull()]
  callableIdIfNonLocal: java/lang/String.intern
  dispatchType: java/lang/String
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: false
  isOverride: false
  isSuspend: false
  modality: OPEN
  name: intern
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
  visibility: Public

KtFirFunctionSymbol:
  annotatedType: [] kotlin/Char
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: kotlin/CharSequence.get
  dispatchType: kotlin/CharSequence
  isExtension: false
  isExternal: false
  isInline: false
  isOperator: true
  isOverride: false
  isSuspend: false
  modality: ABSTRACT
  name: get
  origin: LIBRARY
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(index)]
  visibility: Public

KtFirNamedClassOrObjectSymbol:
  annotationClassIds: []
  annotations: []
  classIdIfNonLocal: java/lang/String.CaseInsensitiveComparator
  classKind: CLASS
  companionObject: null
  isData: false
  isExternal: false
  isFun: false
  isInline: false
  isInner: false
  modality: OPEN
  name: CaseInsensitiveComparator
  origin: JAVA
  superTypes: [[] kotlin/Any, [] java/util/Comparator<ft<kotlin/String, kotlin/String?>>, [] java/io/Serializable]
  symbolKind: MEMBER
  typeParameters: []
  visibility: Private

KtFirConstructorSymbol:
  annotatedType: [] java/lang/String
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: java/lang/String
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: []
  visibility: Public

KtFirConstructorSymbol:
  annotatedType: [] java/lang/String
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: java/lang/String
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(original)]
  visibility: Public

KtFirConstructorSymbol:
  annotatedType: [] java/lang/String
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: java/lang/String
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(value)]
  visibility: Public

KtFirConstructorSymbol:
  annotatedType: [] java/lang/String
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: java/lang/String
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(value), KtFirValueParameterSymbol(offset), KtFirValueParameterSymbol(count)]
  visibility: Public

KtFirConstructorSymbol:
  annotatedType: [] java/lang/String
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: java/lang/String
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(codePoints), KtFirValueParameterSymbol(offset), KtFirValueParameterSymbol(count)]
  visibility: Public

KtFirConstructorSymbol:
  annotatedType: [] java/lang/String
  annotationClassIds: [kotlin/Deprecated]
  annotations: [kotlin/Deprecated(message = Deprecated in Java)]
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: java/lang/String
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(ascii), KtFirValueParameterSymbol(hibyte), KtFirValueParameterSymbol(offset), KtFirValueParameterSymbol(count)]
  visibility: Public

KtFirConstructorSymbol:
  annotatedType: [] java/lang/String
  annotationClassIds: [kotlin/Deprecated]
  annotations: [kotlin/Deprecated(message = Deprecated in Java)]
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: java/lang/String
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(ascii), KtFirValueParameterSymbol(hibyte)]
  visibility: Public

KtFirConstructorSymbol:
  annotatedType: [] java/lang/String
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: java/lang/String
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(bytes), KtFirValueParameterSymbol(offset), KtFirValueParameterSymbol(length), KtFirValueParameterSymbol(charsetName)]
  visibility: Public

KtFirConstructorSymbol:
  annotatedType: [] java/lang/String
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: java/lang/String
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(bytes), KtFirValueParameterSymbol(offset), KtFirValueParameterSymbol(length), KtFirValueParameterSymbol(charset)]
  visibility: Public

KtFirConstructorSymbol:
  annotatedType: [] java/lang/String
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: java/lang/String
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(bytes), KtFirValueParameterSymbol(charsetName)]
  visibility: Public

KtFirConstructorSymbol:
  annotatedType: [] java/lang/String
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: java/lang/String
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(bytes), KtFirValueParameterSymbol(charset)]
  visibility: Public

KtFirConstructorSymbol:
  annotatedType: [] java/lang/String
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: java/lang/String
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(bytes), KtFirValueParameterSymbol(offset), KtFirValueParameterSymbol(length)]
  visibility: Public

KtFirConstructorSymbol:
  annotatedType: [] java/lang/String
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: java/lang/String
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(bytes)]
  visibility: Public

KtFirConstructorSymbol:
  annotatedType: [] java/lang/String
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: java/lang/String
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(buffer)]
  visibility: Public

KtFirConstructorSymbol:
  annotatedType: [] java/lang/String
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: java/lang/String
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(builder)]
  visibility: Public

KtFirConstructorSymbol:
  annotatedType: [] java/lang/String
  annotationClassIds: []
  annotations: []
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: java/lang/String
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(value), KtFirValueParameterSymbol(share)]
  visibility: PackageVisibility

KtFirConstructorSymbol:
  annotatedType: [] java/lang/String
  annotationClassIds: [kotlin/Deprecated]
  annotations: [kotlin/Deprecated(message = Deprecated in Java)]
  callableIdIfNonLocal: null
  containingClassIdIfNonLocal: java/lang/String
  dispatchType: null
  isExtension: false
  isPrimary: false
  origin: JAVA
  receiverType: null
  symbolKind: MEMBER
  typeParameters: []
  valueParameters: [KtFirValueParameterSymbol(offset), KtFirValueParameterSymbol(count), KtFirValueParameterSymbol(value)]
  visibility: PackageVisibility
*/
