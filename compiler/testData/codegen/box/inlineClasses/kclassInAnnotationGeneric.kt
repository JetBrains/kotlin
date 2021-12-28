// WITH_REFLECT
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter
package test

import kotlin.reflect.KClass

OPTIONAL_JVM_INLINE_ANNOTATION
value class ICInt<T: Int>(val i: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ICIntArray(val i: IntArray)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ICIntN<T: Int?>(val i: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ICIntNArray(val i: Array<Int?>)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ICAny<T: Any>(val a: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ICAnyArray(val a: Array<Any>)

OPTIONAL_JVM_INLINE_ANNOTATION
value class ICAnyNArray(val a: Array<Any?>)

annotation class Ann(val c: KClass<*>)
annotation class AnnArray(val c: Array<KClass<*>>)

@Ann(ICInt::class)
@AnnArray([ICInt::class])
class CInt

@Ann(ICIntArray::class)
@AnnArray([ICIntArray::class])
class CIntArray

@Ann(ICIntN::class)
@AnnArray([ICIntN::class])
class CIntN

@Ann(ICIntNArray::class)
@AnnArray([ICIntNArray::class])
class CIntNArray

@Ann(ICAny::class)
@AnnArray([ICAny::class])
class CAny

@Ann(ICAnyArray::class)
@AnnArray([ICAnyArray::class])
class CAnyArray

@Ann(Result::class)
@AnnArray([Result::class])
class CResult

@Ann(ICAnyNArray::class)
@AnnArray([ICAnyNArray::class])
class CAnyNArray

fun box(): String {
    var klass = (CInt::class.annotations.first() as Ann).c.toString()
    if (klass != "class test.ICInt") return "Expected class test.ICInt, got $klass"

    klass = (CIntArray::class.annotations.first() as Ann).c.toString()
    if (klass != "class test.ICIntArray") return "Expected class test.ICIntArray, got $klass"

    klass = (CIntN::class.annotations.first() as Ann).c.toString()
    if (klass != "class test.ICIntN") return "Expected class test.ICIntN, got $klass"

    klass = (CIntNArray::class.annotations.first() as Ann).c.toString()
    if (klass != "class test.ICIntNArray") return "Expected class test.ICIntNArray, got $klass"

    klass = (CAny::class.annotations.first() as Ann).c.toString()
    if (klass != "class test.ICAny") return "Expected class test.ICAny, got $klass"

    klass = (CAnyArray::class.annotations.first() as Ann).c.toString()
    if (klass != "class test.ICAnyArray") return "Expected class test.ICAnyArray, got $klass"

    klass = (CResult::class.annotations.first() as Ann).c.toString()
    if (klass != "class kotlin.Result") return "Expected class kotlin.Result, got $klass"

    klass = (CAnyNArray::class.annotations.first() as Ann).c.toString()
    if (klass != "class test.ICAnyNArray") return "Expected class test.ICAnyNArray, got $klass"


    klass = (CInt::class.annotations.last() as AnnArray).c[0].toString()
    if (klass != "class test.ICInt") return "Expected class test.ICInt, got $klass"

    klass = (CIntArray::class.annotations.last() as AnnArray).c[0].toString()
    if (klass != "class test.ICIntArray") return "Expected class test.ICIntArray, got $klass"

    klass = (CIntN::class.annotations.last() as AnnArray).c[0].toString()
    if (klass != "class test.ICIntN") return "Expected class test.ICIntN, got $klass"

    klass = (CIntNArray::class.annotations.last() as AnnArray).c[0].toString()
    if (klass != "class test.ICIntNArray") return "Expected class test.ICIntNArray, got $klass"

    klass = (CAny::class.annotations.last() as AnnArray).c[0].toString()
    if (klass != "class test.ICAny") return "Expected class test.ICAny, got $klass"

    klass = (CAnyArray::class.annotations.last() as AnnArray).c[0].toString()
    if (klass != "class test.ICAnyArray") return "Expected class test.ICAnyArray, got $klass"

    klass = (CResult::class.annotations.last() as AnnArray).c[0].toString()
    if (klass != "class kotlin.Result") return "Expected class kotlin.Result, got $klass"

    klass = (CAnyNArray::class.annotations.last() as AnnArray).c[0].toString()
    if (klass != "class test.ICAnyNArray") return "Expected class test.ICAnyNArray, got $klass"

    return "OK"
}