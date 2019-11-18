// IGNORE_BACKEND_FIR: JVM_IR
class Test(val prop: String) {

  companion object {
    public val prop : String = "CO";
  }

}


fun box() : String {
  val obj = Test("OK");

  if (Test.prop != "CO") return "fail1";

  return obj.prop;
}
