// NAME: Aliased
open class Outer{
    open class Nested
}

fun f2(){
    val o3 = Outer.Nested() // (1)
}

val g3: <caret>Outer.Nested = Outer.Nested() //(2)

fun f3(p: Outer.Nested): Outer.Nested = p

class Outer3 : Outer.Nested()
class Outer2 {
    class Nested2 : Outer.Nested()
}