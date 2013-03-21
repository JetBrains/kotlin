class Works() : jet.Function0<Object> {
    public override fun invoke():Object {
      return "Works" as Object
    }
}
class Broken() : jet.Function0<String> {
    public override fun invoke():String {
      return "Broken"
    }
}

fun box(): String {
  val works1: ()->Object = Works();
  works1()

  val broken1: ()->String = Broken();
  broken1()

  return "OK"
}
