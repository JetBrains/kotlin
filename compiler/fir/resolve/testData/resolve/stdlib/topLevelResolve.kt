fun testPlus() {
    val x = 1 + 2
    val y = 3.0 + 4.0
    val z = 5 + 6.0
    val w = 7.0 + 8
    val c = 'a' + 1
    val s = "." + ".."
    val ss = "" + 1
    val list = listOf(1, 2, 3) + 4
    val listAndList = listOf(4, 5, 6) + listOf(7, 8)
    val mutableList = mutableListOf(9, 10) + listOf(11, 12, 13)
    val setAndList = setOf(0) + listOf(1, 2)
    val stringAndList = "" + emptyList<Boolean>()
    val map = mapOf("" to 1, "." to 2) + (".." to 3)
    val mapAndMap = mapOf("-" to 4) + mapOf("_" to 5)
}