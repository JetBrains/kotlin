// EMIT_JVM_TYPE_ANNOTATIONS
// RENDER_ANNOTATIONS
// TARGET_BACKEND: JVM_IR

package foo

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val name: String)

@Target(AnnotationTarget.TYPE)
annotation class ClassTypeAnn(val name: String)

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class TypeAnnBinary

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class TypeAnnSource

@Target( AnnotationTarget.TYPE_PARAMETER)
annotation class TypeParameterAnn(val name: String)

@Target(AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class TypeParameterAnnBinary

@Target(AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class TypeParameterAnnSource

interface Generic<Z>
class GenericClass<Z>
class B<Y>

class Kotlin {

    fun <@TypeParameterAnn("TP1") @TypeParameterAnnBinary @TypeParameterAnnSource T, @TypeParameterAnn("TP2") @TypeParameterAnnBinary @TypeParameterAnnSource T2> typeParameter() {
    }

    fun <@TypeParameterAnn("TP") T> genericParameterAndReturn(s: @TypeAnn("1") @TypeAnnBinary @TypeAnnSource T): @TypeAnn("2") @TypeAnnBinary @TypeAnnSource T {
        return s
    }

    fun <@TypeParameterAnn("Y") Y, @TypeParameterAnn("T") T: @TypeAnn("Generic") @TypeAnnBinary @TypeAnnSource Generic<@TypeAnn("Generic Argument") @TypeAnnBinary @TypeAnnSource Y>> genericInterfaceBound() {
    }

    fun <@TypeParameterAnn("Y") Y, @TypeParameterAnn("T") T: @TypeAnn("Generic") @TypeAnnBinary @TypeAnnSource GenericClass<@TypeAnn("Generic Argument") @TypeAnnBinary @TypeAnnSource Y>> genericClassBound() {
    }

    fun <@TypeParameterAnn("Y") Y, @TypeParameterAnn("T") T: @TypeAnn("Generic") Generic<@TypeAnn("Generic Argument") Y>> whereClause() where T : @ClassTypeAnn("Any") Any {

    }

    fun <@TypeParameterAnn("Y") Y, @TypeParameterAnn("T") T: @TypeAnn("Y as Bound") @TypeAnnBinary @TypeAnnSource Y> typeParameterTypeParameterBound() {
    }
}
