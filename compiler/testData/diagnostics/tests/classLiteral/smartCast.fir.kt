// KT-16291 Smart cast doesn't work when getting class of instance

import kotlin.reflect.KClass

class Foo {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null || other::class != this::class) return false

        return true
    }
}

fun test(f: Foo?): KClass<out Foo>? = if (f != null) f::class else null

fun test2(): KClass<out Foo>? {
    var f: Foo? = null
    if (f != null) {
        run { f = null }
        return <!RETURN_TYPE_MISMATCH!><!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>f<!>::class<!>
    }
    return null
}
