// FIR_IDENTICAL
@file:OptIn(ExperimentalAssociatedObjects::class)

import kotlin.reflect.AssociatedObjectKey
import kotlin.reflect.ExperimentalAssociatedObjects
import kotlin.reflect.KClass
import kotlin.reflect.findAssociatedObject

@AssociatedObjectKey
@Retention(AnnotationRetention.BINARY)
annotation class X(val kClass: KClass<*>)

object Promise

<!ASSOCIATED_OBJECT_INVALID_BINDING!>@X(Promise::class)<!>
external class Y

external class OuterExternal {
    <!ASSOCIATED_OBJECT_INVALID_BINDING!>@X(Promise::class)<!>
    class NestedExternal
}
