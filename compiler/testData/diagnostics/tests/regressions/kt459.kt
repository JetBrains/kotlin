// KT-459 Type argument inference fails when class names are fully qualified

fun test() {
  val attributes : java.util.HashMap<String, String> = java.util.HashMap() // failure!
  attributes["href"] = "1" // inference fails, but it shouldn't
}

fun <K, V> Map<K, V>.set(key : K, value : V) {}//= this.put(key, value)