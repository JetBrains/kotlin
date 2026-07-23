// LANGUAGE: +ContextParameters
// OPT_IN: kotlin.ExperimentalContextParameters
// TARGET_BACKEND: JVM_IR
// WITH_REFLECT

import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.test.assertEquals

class C(val tag: String) {
    var storage = ""

    context(c: String)
    fun member(x: Int): String = tag + c + x

    context(c: String)
    val prop: String get() = tag + c

    context(c: String)
    var mutable: String
        get() = storage
        set(value) {
            storage = c + value
        }

    context(c: String)
    fun String.memberExt(): String = tag + c + this
}

fun box(): String {
    val members = C::class.members

    val member = members.single { it.name == "member" } as KFunction<*>
    assertEquals(
        listOf(KParameter.Kind.INSTANCE, KParameter.Kind.CONTEXT, KParameter.Kind.VALUE),
        member.parameters.map { it.kind },
    )
    assertEquals("tctx1", member.call(C("t"), "ctx", 1))

    val prop = members.single { it.name == "prop" } as KProperty<*>
    assertEquals(listOf(KParameter.Kind.INSTANCE, KParameter.Kind.CONTEXT), prop.parameters.map { it.kind })
    assertEquals("tctx", prop.call(C("t"), "ctx"))
    assertEquals("tctx", prop.getter.call(C("t"), "ctx"))

    val mutable = members.single { it.name == "mutable" } as KMutableProperty<*>
    val receiver = C("t")
    mutable.setter.call(receiver, "ctx", "V")
    assertEquals("ctxV", mutable.getter.call(receiver, "ctx"))

    // member extension function: INSTANCE, CONTEXT and EXTENSION_RECEIVER are all unbound
    val memberExt = members.single { it.name == "memberExt" } as KFunction<*>
    assertEquals(
        listOf(KParameter.Kind.INSTANCE, KParameter.Kind.CONTEXT, KParameter.Kind.EXTENSION_RECEIVER),
        memberExt.parameters.map { it.kind },
    )
    assertEquals("tctxR", memberExt.call(C("t"), "ctx", "R"))

    return "OK"
}
