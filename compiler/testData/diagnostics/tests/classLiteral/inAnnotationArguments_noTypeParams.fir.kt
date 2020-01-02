// !LANGUAGE: -ProhibitTypeParametersInClassLiteralsInAnnotationArguments

import kotlin.reflect.KClass

annotation class Ann(vararg val k: KClass<*>)

inline val <reified T> T.test
    get() = @Ann(
        T::class,
        Array<T>::class,
        Array<Array<Array<T>>>::class
    ) object {}
