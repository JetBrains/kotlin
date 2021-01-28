package test

fun main() {
    val toSet = setOf<Int>()
    val clusterToReports = toSet.groupBy { it }
    val updatedClusters = mutableListOf<Int>()
    //Breakpoint!
    updatedClusters.addAll(clusterToReports.map { it.value[0] }) // step over here
    test(updatedClusters)
}

fun test(a: Any) {

}