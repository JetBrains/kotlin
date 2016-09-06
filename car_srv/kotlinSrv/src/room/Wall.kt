package room

import geometry.Line

class Wall constructor(val line: Line, val xFrom: Int, val xTo: Int, val yFrom: Int, val yTo: Int) {

    companion object {
        fun wallFromXml(wall: dynamic): Wall? {
            val points = wall.attributes
            val startX: Int
            val endX: Int
            val endY: Int
            val startY: Int
            try {
                startX = parseInt(points.startX)
                startY = parseInt(points.startY)
                endX = parseInt(points.endX)
                endY = parseInt(points.endY)
            } catch (e: NumberFormatException) {
                return null
            }
            val line = Line((startY - endY).toDouble(), (endX - startX).toDouble(), (startX * endY - startY * endX).toDouble())
            return Wall(line, startX, endX, startY, endY)
        }
    }

    fun wallToString(): String {
        return "xFrom: $xFrom, yFrom: $yFrom, xTo: $xTo, yTo: $yTo"
    }
}
