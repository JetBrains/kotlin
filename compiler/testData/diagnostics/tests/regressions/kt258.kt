// KT-258 Support equality constraints in type inference

import java.util.*

fun test() {
  val attributes : HashMap<String, String> = HashMap()
  attributes["href"] = "1" // inference fails, but it shouldn't
}

fun <K, V> java.util.Map<K, V>.set(<!UNUSED_PARAMETER!>key<!> : K, <!UNUSED_PARAMETER!>value<!> : V) {}//= this.put(key, value)
