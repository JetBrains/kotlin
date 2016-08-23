package geometry

class Vector constructor(val x: Double, val y: Double) {

    constructor(x1: Double, y1: Double, x2: Double, y2: Double) : this(x2 - x1, y2 - y1)


    fun scalarProduct(vector: Vector): Double {
        return this.x * vector.x + this.y * vector.y
    }

}