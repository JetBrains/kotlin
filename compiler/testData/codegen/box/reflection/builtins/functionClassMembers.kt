// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNull

fun interface FunInterface : () -> Int
fun suspendFunction(): suspend () -> Int = null!!

private fun check(expectedInvoke: String, klass: KClass<*>) {
    val members = klass.members
    assertEquals(setOf("equals", "hashCode", "toString", "invoke"), members.map { it.name }.toSet())
    assertEquals(expectedInvoke, members.single { it.name == "invoke" }.toString())
}

fun box(): String {
    check("fun () -> R.invoke(): R", Function0::class)
    check("fun (P1) -> R.invoke(P1): R", Function1::class)
    check("fun FunInterface.invoke(): kotlin.Int", FunInterface::class)

    val suspendFun = ::suspendFunction.returnType.classifier
    check("fun (P1) -> R.invoke(P1): R", suspendFun as KClass<*>)
    return "OK"
}
