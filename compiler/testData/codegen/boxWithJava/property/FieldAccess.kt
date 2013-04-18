class MyWrongClass : FieldAccess() {

}

fun box() : String {
  val clazz = MyWrongClass()
  clazz.fieldO = "O"
  FieldAccess.fieldK = "K"
  return clazz.fieldO!! + FieldAccess.fieldK!!
}