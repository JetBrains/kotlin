// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR, JS, NATIVE
// WITH_REFLECT
package test

import kotlin.test.assertEquals

enum class TestEnum(val id: String? = null) {
    ENUM1(id = "enum1_id"),

    ENUM2(id = "enum2_id") {
        override fun test() {
            ENUM1.test()
        }
    };

    open fun test() {
    }
}

fun box(): String {
    assertEquals(listOf("fun <init>(kotlin.String?): test.TestEnum"), TestEnum.ENUM1::class.constructors.map { it.toString() })
    assertEquals(listOf(), TestEnum.ENUM2::class.constructors.map { it.toString() })

    return "OK"
}
