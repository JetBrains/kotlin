import kotlin.reflect.KClass

annotation class Ann(val value: KClass<*>)

@Ann(<error descr="[ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL] An annotation argument must be a class literal (T::class)">Array<<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: String123">String123</error>>::class</error>) class A
