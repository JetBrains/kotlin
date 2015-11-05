import test.*

fun box(): String {
    val loci = listOf("a", "b", "c")
    var gene = "g1"

    inlineCall {
        val value = 10.0
        loci.forEach {
            var locusMap = 1.0
            {
                locusMap = value
                gene = "OK"
            }()
        }
    }
    return gene
}
