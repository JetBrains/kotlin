// FIR_IDENTICAL
fun <T> checkSubtype(t: T) = t

fun test() {
  checkSubtype<Byte>(1)
  checkSubtype<Int>(1)
  checkSubtype<Double>(<error>1</error>)
  1 <warning>as</warning> Byte
  1 <warning>as Int</warning>
  1 <warning>as</warning> Double
}