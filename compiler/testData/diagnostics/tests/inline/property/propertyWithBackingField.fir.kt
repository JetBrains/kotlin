// WITH_REFLECT
// WITH_STDLIB

import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = 1
}

open class A {
    inline <!INLINE_PROPERTY_WITH_BACKING_FIELD!>val z1<!> = 1

    <!INLINE_PROPERTY_WITH_BACKING_FIELD!>val z1_1<!> = 1
        inline get() = field + 1

    inline <!INLINE_PROPERTY_WITH_BACKING_FIELD!>var z2<!> = 1

    <!INLINE_PROPERTY_WITH_BACKING_FIELD!>var z2_1<!> = 1
        inline set(p: Int) {}

    inline <!INLINE_PROPERTY_WITH_BACKING_FIELD!>val z<!> by Delegate()
}
