package kt_250_617_10

import java.util.ArrayList
import java.util.HashMap

//KT-250 Incorrect variable resolve in constructor arguments of superclass
open class A(val x: Int)
class B(y: Int) : A(x)  //x is resolved as a property in a, so no error is generated

//KT-617 Prohibit dollars in call to superclass constructors
open class M(p: Int)
class N(val p: Int) : A(<!SYNTAX!><!SYNTAX!><!>$p<!><!SYNTAX!>)<!>

//KT-10 Don't allow to use properties in supertype initializers
open class Element()
class TextElement(name: String) : Element()

abstract class Tag(val name : String) {
  val children = ArrayList<Element>()
  val attributes = HashMap<String, String>()
}

abstract class TagWithText(name : String) : Tag(name) {
  operator fun String.unaryPlus() {
    children.add(TextElement(this))
  }
}

open class BodyTag(name : String) : TagWithText(name) {
}

class Body() : BodyTag(name) { // Must be an error!
}
class Body1() : BodyTag(this.name) { // Must be an error!
}

//more tests

open class X(p: Int, r: Int) {
    val s = "s"
}

class Y(i: Int) : X(i, rrr) {
    val rrr = 3
}

class Z(val i: Int) : <!INAPPLICABLE_CANDIDATE!>X<!>(s, x) {
    val x = 2
}