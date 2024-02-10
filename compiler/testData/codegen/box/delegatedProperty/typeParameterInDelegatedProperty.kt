// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// DUMP_IR
// ISSUE: KT-62884

import kotlin.reflect.KProperty
import java.lang.reflect.Type

interface IDelegate<T> {
    operator fun getValue(t: T, p: KProperty<*>): T
}

val <T> T.property by object : IDelegate<T> {
    override fun getValue(t: T, p: KProperty<*>): T {
        return t
    }
}

val <T> T.property2 by run<IDelegate<T>> {
    fun test(t: T): T {
        return t
    }

    object : IDelegate<T> {
        override fun getValue(t: T, p: KProperty<*>): T {
            return test(t)
        }
    }
}

fun box(): String {
    val superInterfaces: Array<Type> = Class.forName("TypeParameterInDelegatedPropertyKt\$property\$2").getGenericInterfaces()
    if (!superInterfaces[0].toString().contains("IDelegate<java.lang.Object>")) return "FAIL"

    val superInterfaces2: Array<Type> = Class.forName("TypeParameterInDelegatedPropertyKt\$property2\$2\$1").getGenericInterfaces()
    if (!superInterfaces2[0].toString().contains("IDelegate<java.lang.Object>")) return "FAIL"

    if ("OK".property2 != "OK") return "FAIL"

    return "OK"
}