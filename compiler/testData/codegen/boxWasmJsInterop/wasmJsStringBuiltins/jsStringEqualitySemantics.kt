// TARGET_BACKEND: WASM
// WITH_STDLIB

@JsFun("""
(s1, s2) => {
  if (typeof s1 !== "string") return "Fail1";
  if (typeof s2 !== "string") return "Fail2";
  if (s1 instanceof String) return "Fail3";
  if (s2 instanceof String) return "Fail4";
  if (!(s1 === s2)) return "Fail5";
  if (!Object.is(s1, s2)) return "Fail6";
  return "OK";
}
""")
external fun jsCheckEqSemantics(s1: String, s2: String): String

fun box(): String {
    val a = "🚀\u0000e\u0301"
    val r = jsCheckEqSemantics(a, a)
    return if (r == "OK") "OK" else r
}
