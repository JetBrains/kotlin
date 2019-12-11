open class Base {
    open var x: Int

    open var y: Int

    constructor() {
        x = 42
        this.y = 24
        val temp = this.x
        this.x = y
        y = temp

    }
}
