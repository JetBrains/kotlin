// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class AsAny<T>(val a: T) {
    fun myEq(other: Any?): Boolean {
        return other is AsAny<*> && other.a == a
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class AsAny2<T: Any>(val a: T?) {
    fun myEq(other: Any?): Boolean {
        return other is AsAny2<*> && other.a == a
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class AsInt<T: Int>(val a: T) {
    fun myEq(other: Any?): Boolean {
        return other is AsInt<*> && other.a == a
    }
}

inline fun <reified T> Any?.isCheck() = this is T

object Reference {
    fun <T> isNullable(a: AsAny<T>) = a is AsAny<*>?
    fun <T> isNotNullable(a: AsAny<T>) = a is AsAny<*>
    fun <T> isNullableNullable(a: AsAny<T>?) = a is AsAny<*>?
    fun <T> isNullableNotNullable(a: AsAny<T>?) = a is AsAny<*>
}

object Reference2 {
    fun <T: Any> isNullable(a: AsAny2<T>) = a is AsAny2<*>?
    fun <T: Any> isNotNullable(a: AsAny2<T>) = a is AsAny2<*>
    fun <T: Any> isNullableNullable(a: AsAny2<T>?) = a is AsAny2<*>?
    fun <T: Any> isNullableNotNullable(a: AsAny2<T>?) = a is AsAny2<*>
}

object Primitive {
    fun <T: Int> isNullable(a: AsInt<T>) = a is AsInt<*>?
    fun <T: Int> isNotNullable(a: AsInt<T>) = a is AsInt<*>
    fun <T: Int> isNullableNullable(a: AsInt<T>?) = a is AsInt<*>?
    fun <T: Int> isNullableNotNullable(a: AsInt<T>?) = a is AsInt<*>
}

fun box(): String {
    val a = AsAny(42)
    val b = AsAny(40 + 2)
    val a2 = AsAny2(42)
    val b2 = AsAny2(40 + 2)

    if (!a.myEq(b)) return "Fail 1"
    if (a.myEq(42)) return "Fail 2"
    if (a.myEq("other")) return "Fail 3"

    if (!Reference.isNullable(a)) return "Fail 4"
    if (!Reference.isNotNullable(a)) return "Fail 5"
    if (!Reference.isNullableNullable(a)) return "Fail 6"
    if (!Reference.isNullableNullable<Int>(null)) return "Fail 7"
    if (!Reference.isNullableNotNullable(a)) return "Fail 8"
    if (Reference.isNullableNotNullable<Int>(null)) return "Fail 9"

    if (!Reference2.isNullable(a2)) return "Fail 42"
    if (!Reference2.isNotNullable(a2)) return "Fail 52"
    if (!Reference2.isNullableNullable(a2)) return "Fail 62"
    if (!Reference2.isNullableNullable<Int>(null)) return "Fail 72"
    if (!Reference2.isNullableNotNullable(a2)) return "Fail 82"
    if (Reference2.isNullableNotNullable<Int>(null)) return "Fail 92"

    val c = AsInt(42)
    val d = AsInt(40 + 2)
    if (!c.myEq(d)) return "Fail 10"
    if (c.myEq(42)) return "Fail 11"
    if (c.myEq("other")) return "Fail 12"

    if (!Primitive.isNullable(c)) return "Fail 13"
    if (!Primitive.isNotNullable(c)) return "Fail 14"
    if (!Primitive.isNullableNullable(c)) return "Fail 15"
    if (!Primitive.isNullableNullable<Int>(null)) return "Fail 16"
    if (!Primitive.isNullableNotNullable(c)) return "Fail 17"
    if (Primitive.isNullableNotNullable<Int>(null)) return "Fail 18"

    if (!a.isCheck<AsAny<Int>>()) return "Fail 19"
    if (!a.isCheck<AsAny<Int>?>()) return "Fail 20"
    if (a.isCheck<AsInt<Int>>()) return "Fail 21"

    if (!c.isCheck<AsInt<Int>>()) return "Fail 22"
    if (!c.isCheck<AsInt<Int>?>()) return "Fail 23"
    if (c.isCheck<AsAny<Int>>()) return "Fail 24"

    if (!a2.isCheck<AsAny2<Int>>()) return "Fail 192"
    if (!a2.isCheck<AsAny2<Int>?>()) return "Fail 202"
    if (a2.isCheck<AsInt<Int>>()) return "Fail 212"

    return "OK"
}