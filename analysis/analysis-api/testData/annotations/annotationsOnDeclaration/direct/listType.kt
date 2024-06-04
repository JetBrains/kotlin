// COMPILATION_ERRORS

import kotlin.reflect.KClass

annotation class Anno(val value: KClass<*>)

@Anno(List<String>::class)
class F<caret>oo