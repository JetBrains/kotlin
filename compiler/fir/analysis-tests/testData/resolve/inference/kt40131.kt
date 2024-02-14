// ISSUE: KT-40131

import kotlin.reflect.KClass

val <U> KClass<U>.javaImpl: Class<U>
    get() = null!!

val <T : KClass<*>> T.myJava1: Class<*>
    get() = <!DEBUG_INFO_EXPRESSION_TYPE("java.lang.Class<CapturedType(*)>")!>javaImpl<!>

val <E : Any, T : KClass<E>> T.myJava2: Class<E>
    get() = <!DEBUG_INFO_EXPRESSION_TYPE("java.lang.Class<E>")!>javaImpl<!>
