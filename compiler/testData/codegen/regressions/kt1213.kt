package interactive;

class Shape(var height : Double = 1.0, var fillColor : String = "#AAAAAA") {

}

fun box() : String {
    var a : Shape? = Shape()
    a?.height = 1.0
    return "OK"
}