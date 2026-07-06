// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

var state: String = ""

var String.prop: String
    get() = length.toString()
    set(value) { state = this + value }

fun box(): String {
    val prop = String::prop

    assertEquals("3", prop.getter.invoke("abc"))
    assertEquals("5", prop.getter("defgh"))

    assertEquals("<get-prop>", prop.getter.name)
    assertEquals("<set-prop>", prop.setter.name)
    assertEquals("[extension receiver parameter of fun kotlin.String.`<get-prop>`(): kotlin.String]", prop.getter.parameters.toString())
    assertEquals("kotlin.String", prop.getter.returnType.toString())
    assertEquals(
        "[extension receiver parameter of fun kotlin.String.`<set-prop>`(kotlin.String): kotlin.Unit, " +
                "parameter #1 value of fun kotlin.String.`<set-prop>`(kotlin.String): kotlin.Unit]", prop.setter.parameters.toString()
    )
    assertEquals("kotlin.Unit", prop.setter.returnType.toString())

    assertNotEquals(prop.getter.parameters.single(), prop.parameters.single())

    prop.setter("O", "K")

    return state
}
