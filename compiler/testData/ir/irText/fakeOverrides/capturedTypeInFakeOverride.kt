// FIR_IDENTICAL
// ISSUE: KT-70395

// Caused by: kotlin.reflect.jvm.internal.KotlinReflectionInternalError: This method should not be called on kotlin.reflect.jvm.internal.types.ReflectTypeSystemContext@635362ae with a new kotlin-reflect implementation. Please file an issue at https://kotl.in/issue
//     at kotlin.reflect.jvm.internal.types.ReflectTypeSystemContext.shouldNotBeCalled(ReflectTypeSystemContext.kt:362)
//     at kotlin.reflect.jvm.internal.types.ReflectTypeSystemContext.intersectTypes(ReflectTypeSystemContext.kt:318)
//     at kotlin.reflect.jvm.internal.***.types.AbstractTypeChecker.isSubtypeForSameConstructorWithIntersectedTypeArguments(AbstractTypeChecker.kt:480)
// SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK

interface A {
    fun m(x: B<out List<Number>>): Int
}

interface B<T : List<out Number>>

abstract class C : A {
    override fun m(x: B<out List<Number>>): Int = TODO()
}