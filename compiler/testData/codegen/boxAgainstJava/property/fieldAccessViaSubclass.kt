// KT-3492

class MyWrongClass : fieldAccessViaSubclass() {

}

fun box() : String {
  val clazz = MyWrongClass()
  clazz.fieldO = "O"
  fieldAccessViaSubclass.fieldK = "K"
  return clazz.fieldO!! + fieldAccessViaSubclass.fieldK!!
}
