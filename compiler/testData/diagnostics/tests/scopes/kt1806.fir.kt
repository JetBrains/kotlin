//KT-1806 accessing private member in object class/anonymous object is not highlighted as error
package kt1806

object MyObject {
    private var message: String = "'Static'"

}

fun test1() {

    doSmth(MyObject.<!INVISIBLE_REFERENCE!>message<!>)
}

class Test {
  private val MyObject1 = object {
      private var message: String = "'Static'"
  }

  fun test2() {
      doSmth(MyObject1.<!INVISIBLE_REFERENCE!>message<!>)
  }
}

fun doSmth(s: String) = s
