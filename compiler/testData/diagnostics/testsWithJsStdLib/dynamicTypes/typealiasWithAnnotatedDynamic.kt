@Target(AnnotationTarget.TYPE)
annotation class Ann1

@Target(AnnotationTarget.TYPE)
annotation class Ann2

typealias AnnGenList<T> = List<@Ann1 T>
typealias TestAnnGen1 = AnnGenList<dynamic>
typealias TestAnnGen2 = AnnGenList<@Ann2 dynamic>
typealias TestAnnGen3 = <!REPEATED_ANNOTATION, REPEATED_ANNOTATION, REPEATED_ANNOTATION!>AnnGenList<@Ann1 dynamic><!>

fun useAnnGen1(x: TestAnnGen1) = x
fun useAnnGen2(x: TestAnnGen2) = x

fun testUseAnnGen1(x: List<dynamic>) = useAnnGen1(x)
fun testUseAnnGen2(x: List<dynamic>) = useAnnGen2(x)