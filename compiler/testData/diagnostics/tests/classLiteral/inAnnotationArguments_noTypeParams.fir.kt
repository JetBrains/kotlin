// LANGUAGE: -ProhibitTypeParametersInClassLiteralsInAnnotationArguments

import kotlin.reflect.KClass

annotation class Ann(vararg val k: KClass<*>)

inline val <reified T> T.test
    get() = @Ann(
        <!ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR!>T::class<!>,
        <!ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR!>Array<T>::class<!>,
        <!ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR!>Array<Array<Array<T>>>::class<!>
    ) object {}
