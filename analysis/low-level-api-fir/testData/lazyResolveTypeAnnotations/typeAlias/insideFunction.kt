@Target(AnnotationTarget.TYPE)
annotation class TypeAnnWithArg(val arg: String)
const val constant = 0

typealias MyTypeAlias = @TypeAnnWithArg("type: $constant") List<@TypeAnnWithArg("nested type: $constant") List<@TypeAnnWithArg("nested nested type: $constant") Int>>

fun resol<caret>veMe(val param: MyTypeAlias) {}