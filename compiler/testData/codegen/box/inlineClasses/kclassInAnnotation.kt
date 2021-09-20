// WITH_REFLECT
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
package test

import kotlin.reflect.KClass

@JvmInline
value class ICInt(val i: Int)

@JvmInline
value class ICIntN(val i: Int?)

@JvmInline
value class ICAny(val a: Any)

annotation class Ann(val c: KClass<*>)

@Ann(ICInt::class)
class CInt

@Ann(ICIntN::class)
class CIntN

@Ann(ICAny::class)
class CAny

@Ann(Result::class)
class CResult

fun box(): String {
    var klass = (CInt::class.annotations.first() as Ann).c.toString()
    if (klass != "class test.ICInt") return "Expected class test.ICInt, got $klass"

    klass = (CIntN::class.annotations.first() as Ann).c.toString()
    if (klass != "class test.ICIntN") return "Expected class test.ICIntN, got $klass"

    klass = (CAny::class.annotations.first() as Ann).c.toString()
    if (klass != "class test.ICAny") return "Expected class test.ICAny, got $klass"

    klass = (CResult::class.annotations.first() as Ann).c.toString()
    if (klass != "class kotlin.Result") return "Expected class kotlin.Result, got $klass"

    return "OK"
}