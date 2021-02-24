package usage.test

import java.io.Serializable
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction

typealias TA<X> = List<X>

open class B {
    fun inherited(): C<in B>? = null
}

class C<T : Any> : B() {
    fun <S : T> function(s: S): TA<S> = listOf(s)
    var property: Int? = 42
    fun String.extensionFunction(): Int = length
    val <U : T?> U.extensionProperty: Unit get() = Unit
}

fun box(): String {
    val members = C::class.members.joinToString("\n")
    if (members != """
        var usage.test.C<T>.property: kotlin.Int?
        val usage.test.C<T>.(U.)extensionProperty: kotlin.Unit
        fun usage.test.C<T>.function(S): usage.test.TA<S> /* = kotlin.collections.List<S> */
        fun usage.test.C<T>.(kotlin.String.)extensionFunction(): kotlin.Int
        fun usage.test.C<T>.equals(kotlin.Any?): kotlin.Boolean
        fun usage.test.C<T>.hashCode(): kotlin.Int
        fun usage.test.C<T>.inherited(): usage.test.C<in usage.test.B>?
        fun usage.test.C<T>.toString(): kotlin.String
    """.trimIndent())
        return "Fail members toString: $members"


    val c = C::class.createInstance()
    c.property = 239
    val callResult = (C<*>::property).call(c)
    if (callResult != 239)
        return "Fail call: $callResult"


    val stringSuperclasses = String::class.superclasses
    if (stringSuperclasses != listOf(Comparable::class, CharSequence::class, Serializable::class, Any::class))
        return "Fail superclasses: $stringSuperclasses"


    val function = B::inherited
    val javaMethod = function.javaMethod!!
    val kotlinFunction = javaMethod.kotlinFunction!!
    if (function != kotlinFunction)
        return "Fail javaMethod/kotlinFunction:\nfunction=$function\njavaMethod=$javaMethod\nkotlinFunction=$kotlinFunction"


    return "OK"
}

fun main() {
    println(box())
}
