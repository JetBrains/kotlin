// KT-459 Type argument inference fails when class names are fully qualified

fun test() {
  val attributes : java.util.HashMap<String, String> = java.util.HashMap() // failure!
  attributes["href"] = "1" // inference fails, but it shouldn't
}

operator fun <K, V> Map<K, V>.set(<!UNUSED_PARAMETER!>key<!> : K, <!UNUSED_PARAMETER!>value<!> : V) {}//= this.put(key, value)