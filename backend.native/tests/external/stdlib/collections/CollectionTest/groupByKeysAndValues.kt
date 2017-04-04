import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val nameToTeam = listOf("Alice" to "Marketing", "Bob" to "Sales", "Carol" to "Marketing")
    val namesByTeam = nameToTeam.groupBy({ it.second }, { it.first })
    assertEquals(
            listOf(
                    "Marketing" to listOf("Alice", "Carol"),
                    "Sales" to listOf("Bob")
            ),
            namesByTeam.toList())


    val mutableNamesByTeam = nameToTeam.groupByTo(HashMap(), { it.second }, { it.first })
    assertEquals(namesByTeam, mutableNamesByTeam)
}
