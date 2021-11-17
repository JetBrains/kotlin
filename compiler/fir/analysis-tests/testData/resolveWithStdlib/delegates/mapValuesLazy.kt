interface TDat

fun resolve(str: String): TDat = null!!

val recProp by lazy {
    mapOf(
        "" to ""
    ).mapValues {
        resolve(it.value)
    }
}

