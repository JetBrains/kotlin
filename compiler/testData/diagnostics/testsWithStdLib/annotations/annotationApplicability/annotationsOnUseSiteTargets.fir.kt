interface Test {
    @get:JvmStatic
    val a: Int

    @get:JvmName("1")
    val b: Int

    @get:Synchronized
    val c: Int

    @get:JvmOverloads
    val d: Int
}
