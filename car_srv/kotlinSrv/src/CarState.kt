class CarState private constructor() {
    //position
    var x: Int
    var y: Int
    var angle: Int//positive is from OX to OY

    init {
        this.x = 0
        this.y = 0
        this.angle = 0
    }

    //if distance is positive - move forward, else backward
    fun moving(distance: Int) {
        x += (Math.cos(angle.toDouble()) * distance).toInt()
        y += (Math.sin(angle.toDouble()) * distance).toInt()
    }

    //angle positive - rotation left
    fun rotate(angle: Int) {
        this.angle += angle
    }

    companion object {
        val instance = CarState()
    }
}