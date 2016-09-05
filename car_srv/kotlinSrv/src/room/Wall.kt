package room

import geometry.Line

class Wall constructor(val line: Line, val xFrom: Int, val xTo: Int, val yFrom: Int, val yTo: Int) {

    companion object {
        fun wallFromString(wall: String): Wall? {
            val points = wall.replace(" ", "").split(",")
            var xFrom = 0
            var xTo = 0
            var yTo = 0
            var yFrom = 0
            points.forEach {
                val keyValue = it.split(":")
                if (keyValue.size != 2) {
                    return null
                }
                try {
                    when (keyValue[0]) {
                        "xFrom" -> xFrom = parseInt(keyValue[1])
                        "yFrom" -> yFrom = parseInt(keyValue[1])
                        "xTo" -> xTo = parseInt(keyValue[1])
                        "yTo" -> yTo = parseInt(keyValue[1])
                    }
                } catch (e: Exception) {
                    return null
                }
            }
            val line = Line((yFrom - yTo).toDouble(), (xTo - xFrom).toDouble(), (xFrom * yTo - yFrom * xTo).toDouble())
            return Wall(line, xFrom, xTo, yFrom, yTo)
        }
    }

    fun wallToString(): String {
        return "xFrom: $xFrom, yFrom: $yFrom, xTo: $xTo, yTo: $yTo"
    }
}
