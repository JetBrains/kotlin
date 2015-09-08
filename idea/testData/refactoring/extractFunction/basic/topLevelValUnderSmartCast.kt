// SUGGESTED_NAMES: i, getKm
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: public val meters: kotlin.Int? defined in root package
val meters: Int? = 1

fun test() {
    if (meters == null) return
    val km = <selection>meters / 10</selection>
}