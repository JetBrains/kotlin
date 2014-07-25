class Works() : Function0<Any> {
    public override fun invoke():Any {
      return "Works" as Any
    }
}
class Broken() : Function0<String> {
    public override fun invoke():String {
      return "Broken"
    }
}

fun box(): String {
  val works1: ()->Any = Works();
  works1()

  val broken1: ()->String = Broken();
  broken1()

  return "OK"
}
