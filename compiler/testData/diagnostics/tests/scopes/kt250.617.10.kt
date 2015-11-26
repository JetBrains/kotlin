package kt_250_617_10

import java.util.ArrayList
import java.util.HashMap

//KT-250 Incorrect variable resolve in constructor arguments of superclass
open class A(val x: Int)
class B(<!UNUSED_PARAMETER!>y<!>: Int) : A(<!UNRESOLVED_REFERENCE!>x<!>)  //x is resolved as a property in a, so no error is generated

//KT-617 Prohibit dollars in call to superclass constructors
open class M(<!UNUSED_PARAMETER!>p<!>: Int)
class N(val p: Int) : A(<!SYNTAX!><!SYNTAX!><!>$p<!><!SYNTAX!>)<!>

//KT-10 Don't allow to use properties in supertype initializers
open class Element()
class TextElement(<!UNUSED_PARAMETER!>name<!>: String) : Element()

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

class Body() : BodyTag(<!UNRESOLVED_REFERENCE!>name<!>) { // Must be an error!
}
class Body1() : BodyTag(<!NO_THIS!>this<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>name<!>) { // Must be an error!
}

//more tests

open class X(<!UNUSED_PARAMETER!>p<!>: Int, <!UNUSED_PARAMETER!>r<!>: Int) {
    val s = "s"
}

class Y(i: Int) : X(i, <!UNRESOLVED_REFERENCE!>rrr<!>) {
    val rrr = 3
}

class Z(val i: Int) : X(<!UNRESOLVED_REFERENCE!>s<!>, <!UNRESOLVED_REFERENCE!>x<!>) {
    val x = 2
}