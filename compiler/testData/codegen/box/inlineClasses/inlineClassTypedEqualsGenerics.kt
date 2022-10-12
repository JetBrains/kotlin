// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomEqualsInInlineClasses
// TARGET_BACKEND: JVM_IR

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC1<T : Number>(val x: T) {
    fun equals(other: Int) = false
    fun equals(other: IC1<T>) = true
}

class Generic<T, R>(val x: T, val y: R)

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC2<T, R>(val value: Generic<T, R>) {
    fun equals(other: IC1<Double>) = false
    fun equals(other: IC2<T, R>) = true
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC3<T>(val value: T) {
    fun equals(other: Int) = false
    fun equals(other: IC3<*>) = true
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC4<T>(val value: T) {
    fun equals(other: String) = false
    fun equals(other: IC4<String>) = true
}


fun box() = when {
    IC1(5.0) != IC1(3) -> "Fail 1.1"
    (IC1(5.0) as Any) != IC1(3) -> "Fail 1.2"
    IC1(5.0) != (IC1(3) as Any) -> "Fail 1.3"
    (IC1(5.0) as Any) != (IC1(3) as Any) -> "Fail 1.4"

    IC2(Generic("aba", 5.0)) != IC2(Generic(3, 8)) -> "Fail 2.1"
    (IC2(Generic("aba", 5.0)) as Any) != IC2(Generic(3, 8)) -> "Fail 2.2"
    IC2(Generic("aba", 5.0)) != (IC2(Generic(3, 8)) as Any) -> "Fail 2.3"
    (IC2(Generic("aba", 5.0)) as Any) != (IC2(Generic(3, 8)) as Any) -> "Fail 2.4"

    IC3("x") != IC3("y") -> "Fail 3.1"
    (IC3("x") as Any) != IC3("y") -> "Fail 3.2"
    IC3("x") != (IC3("y") as Any) -> "Fail 3.3"
    (IC3("x") as Any) != (IC3("y") as Any) -> "Fail 3.4"

    IC4("aba") != IC4("caba") -> "Fail 4.1"
    (IC4("aba") as Any) != IC4("caba") -> "Fail 4.2"
    IC4("aba") != (IC4("caba") as Any) -> "Fail 4.3"
    (IC4("aba") as Any) != (IC4("caba") as Any) -> "Fail 4.4"

    else -> "OK"
}