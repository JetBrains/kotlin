import kotlin.reflect.KClass

annotation class Ann(val a: Array<KClass<*>>)


inline val <reified T> T.test
    get() = @Ann(
        <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>arrayOf(
            <!ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR!>T::class<!>,
            <!ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR!>Array<Array<Array<Array<T>>>>::class<!>
        )<!>
    ) object {}
