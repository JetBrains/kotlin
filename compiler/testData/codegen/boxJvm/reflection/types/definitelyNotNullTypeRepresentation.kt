// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.*

interface WithDnn<T> {
    fun accept(x: T & Any): T & Any
    fun acceptNullable(x: T?): T?
    fun <S> generic(x: S & Any): S & Any
}

class Impl<T> : WithDnn<T> {
    override fun accept(x: T & Any): T & Any = x
    override fun acceptNullable(x: T?): T? = x
    override fun <S> generic(x: S & Any): S & Any = x
}

fun box(): String {
    val accept = WithDnn::class.members.single { it.name == "accept" }
    val acceptNullable = WithDnn::class.members.single { it.name == "acceptNullable" }
    val generic = WithDnn::class.members.single { it.name == "generic" }

    // T & Any parameter — definitely not null type
    val dnnParam = accept.parameters.last()
    assertFalse(dnnParam.type.isMarkedNullable,
        "T & Any should not be marked nullable; was: ${dnnParam.type}")
    // The string representation should contain & (intersection) notation
    val dnnStr = dnnParam.type.toString()
    assertTrue(dnnStr.contains("&"),
        "Expected '&' in T & Any toString, got: $dnnStr")

    // Return type of accept is also T & Any
    val dnnReturn = accept.returnType
    assertFalse(dnnReturn.isMarkedNullable)
    assertTrue(dnnReturn.toString().contains("&"),
        "Expected '&' in return type, got: ${dnnReturn}")

    // T? parameter is nullable but not DNN
    val nullableParam = acceptNullable.parameters.last()
    assertTrue(nullableParam.type.isMarkedNullable,
        "T? should be marked nullable")
    assertFalse(nullableParam.type.toString().contains("&"))

    // Generic method: S & Any on its own type parameter
    val genericParam = generic.parameters.last()
    assertFalse(genericParam.type.isMarkedNullable)
    assertTrue(genericParam.type.toString().contains("&"))

    // Implementation class inherits the DNN types
    val implAccept = Impl::class.memberFunctions.firstOrNull { it.name == "accept" }
    assertNotNull(implAccept)
    val implDnnParam = implAccept.parameters.last()
    assertFalse(implDnnParam.type.isMarkedNullable)

    return "OK"
}
