@Target(AnnotationTarget.TYPE)
annotation class TypeAnnWithArg(val arg: String)

typealias BadArg<caret>sInTypeAlias = @TypeAnnWithArg List<@TypeAnnWithArg List<@TypeAnnWithArg Int>>