// KT-394 Make class object members visible inside the owning class

class X() {
//    class Y {}

    class object{
        class Y() {}
    }

    val y : Y = Y()
}
