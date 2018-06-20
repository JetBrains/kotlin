// NAME: Aliased
open class Outer{
    open class Middle {
        open class Nested
    }
}

fun f2(){
    val o3 = Outer.Middle.Nested() // (1)
}

val g3: <caret>Outer.Middle.Nested = Outer.Middle.Nested() //(2)

fun f3(p: Outer.Middle.Nested): Outer.Middle.Nested = p

class Outer3 : Outer.Middle.Nested()
class Outer2 {
    class Nested2 : Outer.Middle.Nested()
}