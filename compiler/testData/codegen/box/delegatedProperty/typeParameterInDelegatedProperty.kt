// TARGET_BACKEND: JVM_IR
// DUMP_IR
// ISSUE: KT-62884

import kotlin.reflect.KProperty
import java.lang.reflect.Type

interface IDelegate<T> {
    operator fun getValue(t: T, p: KProperty<*>)
}

val <T> T.property by object : IDelegate<T> {
    val <K> K.property2 by object : IDelegate<K> {
        override fun getValue(k: K, p: KProperty<*>) {
            val k1: K? = null
            val t1: T? = null
        }
    }

    override fun getValue(t: T, p: KProperty<*>) {
    }
}

fun box(): String {
    val clazz = Class.forName("TypeParameterInDelegatedPropertyKt\$property\$2")
    val superInterfaces: Array<Type> = clazz.getGenericInterfaces()
    return if (superInterfaces[0].toString() == "IDelegate<java.lang.Object>") "OK" else "FAIL"
}