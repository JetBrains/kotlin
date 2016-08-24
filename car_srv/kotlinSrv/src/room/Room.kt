package room

import geometry.Line

object Room {


    /*
    _________
    |        |
    |        |
    |________|

     */

//    val upLine = Line(0.0, 1.0, -300.0)
//    val leftLine = Line(1.0, 0.0, 150.0)
//    val bottomLine = Line(0.0, 1.0, 20.0)
//    val rightLine = Line(1.0, 0.0, -200.0)
//
//    val walls = listOf<Wall>(Wall(upLine, 200, -150, 300, 300),
//            Wall(leftLine, -150, -150, 300, -20),
//            Wall(bottomLine, -150, 200, -20, -20),
//            Wall(rightLine, 200, 200, -20, 300)
//    )


    /*
    ________
    |       |
    |    ___|
    |___|

    */
    val upLine = Line(0.0, 1.0, -300.0)
    val leftLine = Line(1.0, 0.0, 150.0)
    val bottomLine = Line(0.0, 1.0, 20.0)
    val rightLine = Line(1.0, 0.0, -200.0)

    val bottomLine2 = Line(0.0, 1.0, -100.0)
    val rightLine2 = Line(1.0, 0.0, -300.0)

    val walls = listOf<Wall>(Wall(upLine, 300, -150, 300, 300),
            Wall(leftLine, -150, -150, 300, -20),
            Wall(bottomLine, -150, 200, -20, -20),
            Wall(rightLine, 200, 200, -20, 100),
            Wall(bottomLine2, 200, 300, 100, 100),
            Wall(rightLine2, 300, 300, 100, 300)
    )

    /*

    ________
    |     __|
    |____/


     */

//    val upLine = Line(0.0, 1.0, -300.0)
//    val leftLine = Line(1.0, 0.0, 150.0)
//    val bottomLine = Line(0.0, 1.0, 20.0)
//    val rightLine = Line(-1.2, 1.0, 140.0)
//
//    val bottomLine2 = Line(0.0, 1.0, -100.0)
//    val rightLine2 = Line(1.0, 1.0, -350.0)
//
//    val walls = listOf<Wall>(Wall(upLine, 350, -150, 300, 300),
//            Wall(leftLine, -150, -150, 300, -20),
//            Wall(bottomLine, -150, 100, -20, -20),
//            Wall(rightLine, 100, 200, -20, 100),
//            Wall(bottomLine2, 200, 350, 100, 100),
//            Wall(rightLine2, 350, 350, 100, 300)
//    )

}