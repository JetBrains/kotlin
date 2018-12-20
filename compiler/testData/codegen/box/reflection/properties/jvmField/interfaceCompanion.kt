// !LANGUAGE: +JvmFieldInInterface
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// WITH_REFLECT

import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.companionObject

class Bar(val value: String)

interface Foo {

    companion object {
        @JvmField
        val z = Bar("OK")
    }
}


fun box(): String {
    val field = Foo::class.companionObject!!.memberProperties.single() as KProperty1<Foo.Companion, Bar>
    return field.get(Foo.Companion).value
}
