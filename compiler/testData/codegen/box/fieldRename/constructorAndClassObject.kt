class Test(val prop: String) {

  default object {
    public val prop : String = "CO";
  }

}


fun box() : String {
  val obj = Test("OK");

  if (Test.prop != "CO") return "fail1";

  return obj.prop;
}
