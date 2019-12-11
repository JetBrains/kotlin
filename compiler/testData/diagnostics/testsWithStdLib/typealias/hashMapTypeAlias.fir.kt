import kotlin.test.*
import java.util.*

typealias HM<Kt, Vt> = HashMap<Kt, Vt>

fun <K, V, M : MutableMap<in K, MutableList<V>>> updateMap(map: M, k: K, v: V): M {
    map[k]!!.add(v)
    return map
}

val nameToTeam = listOf("Alice" to "Marketing", "Bob" to "Sales", "Carol" to "Marketing")
val namesByTeam = nameToTeam.groupBy({ it.second }, { it.first })

val mutableNamesByTeam1 = updateMap(HM(), "", "")
val mutableNamesByTeam2 = updateMap(HashMap(), "", "")

fun test() {
    assertEquals(namesByTeam, mutableNamesByTeam1)
    assertEquals(namesByTeam, mutableNamesByTeam2)
}
