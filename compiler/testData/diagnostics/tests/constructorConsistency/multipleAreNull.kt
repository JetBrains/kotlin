// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Context
class Point

class Example {
    constructor(context: Context?)
    constructor(context: Context?, arg1: Int)
    constructor(context: Context?, arg1: Int, arg2: Int)
    constructor(context: Context?, arg1: Int, arg2: Int, arg3: Int)

    var condition: Boolean = false
    private var index = <!DEBUG_INFO_LEAKING_THIS!>newIndex<!>(condition)
    private fun newIndex(zero: Boolean) = if (zero) 0 else 1

    private lateinit var latePoint1: Point
    private lateinit var latePoint2: Point

    private val point1 = Point()
    private val point2 = Point()
    private val point3 = Point()
    private val point4 = Point()
    private var nullPoint: Point? = null
}
