@Target(AnnotationTarget.TYPE)
annotation class TypeAnnWithArg(val arg: String)

typealias BadArgs<caret>InTypeAlias = List<@TypeAnnWithArg Int>