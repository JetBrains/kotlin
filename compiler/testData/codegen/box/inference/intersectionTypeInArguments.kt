// TARGET_BACKEND: JVM
// WITH_STDLIB
// WITH_REFLECT

import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType

fun convertPrimitivesArray(type: KType, args: Sequence<String?>): Any? {
    val a = when (type.classifier) {
        IntArray::class -> args.map { it?.toIntOrNull() }
        CharArray::class -> args.map { it?.singleOrNull() }
        else -> null
    }
    val b = a?.toList()
    val c = b?.takeUnless { null in it }
    val d = c?.toTypedArray()
    return d
}

fun box(): String {
    val type = CharArray::class.starProjectedType
    val sequence = sequenceOf("O", "K")
    val array = convertPrimitivesArray(type, sequence) as Array<*>
    return array.joinToString("") { it.toString() }
}
