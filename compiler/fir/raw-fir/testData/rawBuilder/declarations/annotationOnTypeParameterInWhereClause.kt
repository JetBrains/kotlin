@Repeatable
@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class TypeParameterAnn(val name: String)

class Foo<@TypeParameterAnn("T") T> where @TypeParameterAnn("Prohibit me!!!") T : Any {}
