package room

import geometry.Line
import require

class Room() {

    val walls = arrayListOf<Wall>()

    constructor(walls: Collection<Wall>) : this() {
        this.walls.addAll(walls)
    }

    companion object {
        fun roomFromString(roomString: String): Room? {
            val xmlParser = require("xml-parser")
            val xmlObject = xmlParser(roomString)
            val wallsArray: dynamic
            try {
                wallsArray = xmlObject.root.children[0].children
            } catch (e: Exception) {
                return null
            }
            val result = Room()
            for (i in 0..(parseInt(wallsArray.length) - 1)) {
                val wall = Wall.wallFromXml(wallsArray[i]) ?: return null
                result.walls.add(wall)
            }
            return result
        }

        fun testRoom1(): Room {
            val upLine = Line(0.0, 1.0, -300.0)
            val leftLine = Line(1.0, 0.0, 150.0)
            val bottomLine = Line(0.0, 1.0, 45.0)
            val rightLine = Line(1.0, 0.0, -200.0)
            val walls = listOf(Wall(upLine, 200, -150, 300, 300),
                    Wall(leftLine, -150, -150, 300, -45),
                    Wall(bottomLine, -150, 200, -45, -45),
                    Wall(rightLine, 200, 200, -45, 300)
            )
            return Room(walls)
        }

        fun testRoom2(): Room {

            val upLine = Line(0.0, 1.0, -300.0)
            val leftLine = Line(1.0, 0.0, 150.0)
            val bottomLine = Line(0.0, 1.0, 45.0)
            val rightLine = Line(1.0, 0.0, -200.0)

            val bottomLine2 = Line(0.0, 1.0, -100.0)
            val rightLine2 = Line(1.0, 0.0, -300.0)

            val walls = listOf(Wall(upLine, 300, -150, 300, 300),
                    Wall(leftLine, -150, -150, 300, -45),
                    Wall(bottomLine, -150, 200, -45, -45),
                    Wall(rightLine, 200, 200, -45, 100),
                    Wall(bottomLine2, 200, 300, 100, 100),
                    Wall(rightLine2, 300, 300, 100, 300)
            )
            return Room(walls)
        }

        fun testRoom3(): Room {

            val upLine = Line(0.0, 1.0, -300.0)
            val leftLine = Line(1.0, 0.0, 150.0)
            val bottomLine = Line(0.0, 1.0, 20.0)
            val rightLine = Line(-1.2, 1.0, 140.0)
            val rightLine2 = Line(1.0, 0.0, -200.0)

            val walls = listOf(Wall(upLine, 200, -150, 300, 300),
                    Wall(leftLine, -150, -150, 300, -20),
                    Wall(bottomLine, -150, 100, -20, -20),
                    Wall(rightLine, 100, 200, -20, 100),
                    Wall(rightLine2, 200, 200, 100, 300)
            )

            return Room(walls)
        }
    }

    fun roomToString(): String {
        var result = ""
        walls.forEach {
            result += it.wallToString() + "\n"
        }
        return result
    }
}
