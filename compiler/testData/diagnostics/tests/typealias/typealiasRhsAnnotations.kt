@Target(AnnotationTarget.TYPE)
annotation class Ann1

@Target(AnnotationTarget.TYPE)
annotation class Ann2

typealias Alias1 = @Ann1 String
typealias Alias2 = @Ann2 Alias1
fun test1(a: Alias2) = a

// NB @ExtensionFunctionType on non-functional type is not an error
typealias Alias3 = @ExtensionFunctionType Alias1
fun test2(a: Alias3) = a

typealias LA1 = List<@Ann2 Alias1>
fun test3(la1: LA1) = la1
