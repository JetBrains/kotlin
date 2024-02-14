// FIR_IDENTICAL
// FULL_JDK

import java.util.*
import kotlin.collections.HashMap

typealias MyType = String
enum class MyEnum { FOO, BAR }
enum class MyOtherEnum { A, B, C }

private fun buildMapOfMaps(): Map<MyType, Map<MyEnum, MyOtherEnum>> {
    val results = HashMap<Pair<MyType, MyEnum>, MyOtherEnum>()
    return results
        .asSequence()
        .groupingBy { it.key.first }
        .fold(
            { _, _ -> (EnumMap(MyEnum::class.java)) },
            { _, accumulator, element -> accumulator.also { map -> map[element.key.second] = element.value } }
        )
}
