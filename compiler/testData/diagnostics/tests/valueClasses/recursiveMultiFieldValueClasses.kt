// FIR_IDENTICAL
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// LANGUAGE: +ValueClasses


@JvmInline
value class A1(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>A1<!>)

@JvmInline
value class B1(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>B1<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>B1<!>)


@JvmInline
value class A2(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>B2<!>)

@JvmInline
value class B2(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>A2<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>A2<!>)


@JvmInline
value class A3(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>B3<!>)

@JvmInline
value class B3(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>A3<!>)


@JvmInline
value class A4(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>B4<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>B4<!>)

@JvmInline
value class B4(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>A4<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>A4<!>)

@JvmInline
value class C4(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>D4?<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>D4?<!>)

@JvmInline
value class D4(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>D4?<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>C4?<!>)

@JvmInline
value class E4(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>E4?<!>, val y: Int)

@JvmInline
value class F4(val x: Int, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>F4?<!>)



@JvmInline
value class A5<T : A5<T>>(val x: T)

@JvmInline
value class B5<T : B5<T>>(val x: T, val y: T)


@JvmInline
value class A6<T : B6<<!UPPER_BOUND_VIOLATED!>T<!>>>(val x: T, val y: T)

@JvmInline
value class B6<T : A6<<!UPPER_BOUND_VIOLATED!>T<!>>>(val x: T)


@JvmInline
value class A7<T : B7<<!UPPER_BOUND_VIOLATED!>T<!>>>(val x: T, val y: T)

@JvmInline
value class B7<T : A7<<!UPPER_BOUND_VIOLATED!>T<!>>>(val x: T, val y: T)


@JvmInline
value class A8<T : B8<<!UPPER_BOUND_VIOLATED!>T<!>>>(val x: T?, val y: T?)

@JvmInline
value class B8<T : A8<<!UPPER_BOUND_VIOLATED!>T<!>>>(val x: T?, val y: T?)

interface I1
interface I2

@JvmInline
value class A<T, G : C?>(
    val t1: List<T>,
    val t2: UInt,
    val t3: List<G?>,
    val t4: UInt,
    val t5: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>C<!>,
    val t6: Int,
    val t7: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>B<!>,
    val t8: String,
    val t9: T,
    val t10: Char,
    val t11: T?,
) where T : I1, T : B?, T : I2

@JvmInline
value class B(val x: UInt, val a: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>A<B, Nothing><!>) : I1, I2

@JvmInline
value class C(val x: UInt, val a: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>A<B, Nothing><!>)
