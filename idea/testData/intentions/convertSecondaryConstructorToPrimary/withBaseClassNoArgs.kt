interface Interface

interface Another

abstract class Base

class Derived : Interface, Base, Another {

    val x: String

    constructor(x: String<caret>): super() {
        this.x = x
    }
}