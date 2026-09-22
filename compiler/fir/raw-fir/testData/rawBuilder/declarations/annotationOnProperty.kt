annotation class Ann

@field:Ann
val x: Int = 1

@property:Ann
val y: Int = 2

@Ann
val z: Int = 3

class Some(@field:Ann val x: Int, @property: Ann val y: Int, @param:Ann val z: Int, val w: Int) {
    @field:Ann
    val a: Int = 1

    @property:Ann
    val b: Int = 2

    @Ann
    val c: Int = 3
}
