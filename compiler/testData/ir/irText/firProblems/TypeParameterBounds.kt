// FIR_IDENTICAL
// LANGUAGE: +ClassTypeParameterAnnotations

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val name: String)

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class TypeParameterAnn

@Target(AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class TypeParameterAnnBinary

interface Simple
class SimpleClass
interface Generic<G>
class GenericClass<G>

class SimpleParameter<@TypeParameterAnn @TypeParameterAnnBinary T> {}

class InterfaceBound<@TypeParameterAnn T : @TypeAnn("Simple") Simple> {}

class ClassBound<@TypeParameterAnn T : @TypeAnn("Simple") SimpleClass>

class InterfaceBoundGeneric<T : @TypeAnn("Generic") Generic<@TypeAnn("Simple") Simple>> {}

class ClassBoundGeneric<T : @TypeAnn("GenericClass") GenericClass<@TypeAnn("SimpleClass") SimpleClass>>

class TypeParameterAsBound<Y, @TypeParameterAnn T : @TypeAnn("Y as Bound") Y>
