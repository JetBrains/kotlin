class WithProperties {
    val x: Int
    val y: Int
    private val z: Int

    constructor(x: Int, y: Int = 7, z: Int = 13<caret>) {
        this.x = x
        this.y = y
        this.z = z
    }
}