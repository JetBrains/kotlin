// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// FILE: 1.kt
package test

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
interface Base {
}

class BaseImpl : Base {
}

class ObjectContainer {
}

typealias ObjectContainerProvider = () -> ObjectContainer?

internal inline fun <reified T : Base> ObjectContainerProvider.extensionFun(): ReadOnlyProperty<Any, T> {

    return object : ReadOnlyProperty<Any, T> {
        val emptyProxy: T by lazy {
            T::class.java.getDeclaredConstructor().newInstance()
        }

        override fun getValue(thisRef: Any, property: KProperty<*>): T = emptyProxy
    }

}

// FILE: 2.kt
import test.*

class DefaultObjectContainerProvider : ObjectContainerProvider {
    val baseImpl: BaseImpl by extensionFun<BaseImpl>()

    override fun invoke(): ObjectContainer? {
        return null
    }
}

fun box(): String {
    DefaultObjectContainerProvider().baseImpl

    return "OK"
}