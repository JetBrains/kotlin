// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

import kotlin.reflect.KProperty

OPTIONAL_JVM_INLINE_ANNOTATION
value class ICInt<T: Int>(val i: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ICLong<T: Long>(val l: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ICOverIC<T: ICLong<Long>>(val o: T)

class Delegate<T>(var f: () -> T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = f()
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        f = { value }
    }
}

object Demo {
    val i0 by Delegate { ICInt(1) }
    val l0 by Delegate { ICLong(2L) }
    val o0 by Delegate { ICOverIC(ICLong(3L)) }

    val i1: ICInt<Int> by Delegate { ICInt(11) }
    val l1: ICLong<Long> by Delegate { ICLong(22) }
    val o1: ICOverIC<ICLong<Long>> by Delegate { ICOverIC(ICLong(33)) }

    var i2 by Delegate { ICInt(0) }
    var l2 by Delegate { ICLong(0) }
    var o2 by Delegate { ICOverIC(ICLong(0)) }
}

fun box(): String {
    if (Demo.i0.i != 1) return "Fail 1"
    if (Demo.l0.l != 2L) return "Fail 2"
    if (Demo.o0.o.l != 3L) return "Fail 3"

    if (Demo.i1.i != 11) return "Fail 2 1"
    if (Demo.l1.l != 22L) return "Fail 2 2"
    if (Demo.o1.o.l != 33L) return "Fail 2 3"

    Demo.i2 = ICInt(33)
    Demo.l2 = ICLong(33)
    Demo.o2 = ICOverIC(ICLong(33))

    if (Demo.i2.i != 33) return "Fail 3 1"
    if (Demo.l2.l != 33L) return "Fail 3 2"
    if (Demo.o2.o.l != 33L) return "Fail 3 3"

    val localI by Delegate { ICInt(44) }
    val localL by Delegate { ICLong(44) }
    val localO by Delegate { ICOverIC(ICLong(44)) }

    if (localI.i != 44) return "Fail 4 1"
    if (localL.l != 44L) return "Fail 4 2"
    if (localO.o.l != 44L) return "Fail 4 3"

    return "OK"
}