package room

object Data {

    val upLine = Line(0.0, 1.0, -300.0)
    val leftLine = Line(1.0, 0.0, 150.0)
    val bottomLine = Line(0.0, 1.0, 20.0)
    val rightLine = Line(1.0, 0.0, -200.0)

    val walls = listOf<Wall>(Wall(upLine, 200, -150, 300, 300),
            Wall(leftLine, -150, -150, 300, -20),
            Wall(bottomLine, -150, 200, -20, -20),
            Wall(rightLine, 200, 200, -20, 300)
    )

}