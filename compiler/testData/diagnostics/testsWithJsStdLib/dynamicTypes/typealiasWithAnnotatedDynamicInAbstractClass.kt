// FIR_IDENTICAL
@Target(AnnotationTarget.TYPE)
annotation class Ann1

@Target(AnnotationTarget.TYPE)
annotation class Ann2

abstract class AnnGenList<T> : List<@Ann1 T> {}
typealias TestAnnGen1 = AnnGenList<dynamic>
typealias TestAnnGen2 = AnnGenList<@Ann2 dynamic>
typealias TestAnnGen3 = AnnGenList<@Ann1 dynamic> // KT-62602: should REPEATED_ANNOTATION diagnostic be raised here?

fun useAnnGen1(x: TestAnnGen1) = x
fun useAnnGen2(x: TestAnnGen2) = x
