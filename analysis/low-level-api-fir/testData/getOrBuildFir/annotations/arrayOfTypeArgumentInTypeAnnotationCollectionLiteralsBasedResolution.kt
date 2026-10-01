// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtTypeReference
// LANGUAGE: +CollectionLiteralsBasedAnnotationResolution
// ISSUE: KT-81110

val x: @Anno(arrayOf<<expr>String</expr>>()) Int = 0

@Target(AnnotationTarget.TYPE)
annotation class Anno(val a: Array<String>)
