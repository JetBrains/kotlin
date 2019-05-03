// !LANGUAGE: -ProhibitTypeParametersInClassLiteralsInAnnotationArguments

import kotlin.reflect.KClass

annotation class Ann(vararg val k: KClass<*>)

inline val <reified T> T.test
    get() = @Ann(
        <!ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER!>T::class<!>,
        <!ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER!>Array<T>::class<!>,
        <!ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER!>Array<Array<Array<T>>>::class<!>
    ) object {}
