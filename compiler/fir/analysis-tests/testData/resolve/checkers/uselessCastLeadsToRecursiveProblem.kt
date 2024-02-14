// ISSUE: KT-43603

import kotlin.reflect.KClass

interface A
class B : A
open class C : A

val <T : C> KClass<T>.extProp1
    get() = "I'm C"

val A.extProp1
    get() = when (this) {
        is B -> "I'm B"
        is C -> (this <!USELESS_CAST!>as C<!>)::class.extProp1
        else -> "I don't know who I am.."
    }

val <T : C> KClass<T>.extProp2
    get() = "I'm C"

val A.extProp2
    get() = when (this) {
        is B -> "I'm B"
        is C -> this::class.extProp2
        else -> "I don't know who I am.."
    }
