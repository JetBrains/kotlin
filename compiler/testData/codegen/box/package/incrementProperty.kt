// IGNORE_BACKEND_FIR: JVM_IR
class Slot() {
  var vitality: Int = 10000

  fun increaseVitality(delta: Int) {
    vitality += delta
    if (vitality > 65535) vitality = 65535;
  }
}

fun box(): String {
  val s = Slot()
  s.increaseVitality(1000)
  return if (s.vitality == 11000) "OK" else "fail"
}
