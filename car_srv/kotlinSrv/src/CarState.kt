class CarState private constructor() {
    //position
    var x: Double
    var y: Double
    var angle: Double

    init {
        this.x = 0.0
        this.y = 0.0
        this.angle = 0.0
    }

    companion object {
        val instance = CarState()
    }
}