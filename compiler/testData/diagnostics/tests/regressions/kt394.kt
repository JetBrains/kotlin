// KT-394 Make default object members visible inside the owning class

class X() {
//    class Y {}

    default object{
        class Y() {}
    }

    val y : Y = Y()
}
