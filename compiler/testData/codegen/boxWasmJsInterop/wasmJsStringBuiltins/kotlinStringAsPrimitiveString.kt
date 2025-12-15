// TARGET_BACKEND: WASM
// WITH_STDLIB


@JsFun("""
(s) => {
  if (typeof s !== "string") return "Fail1";
  if (s instanceof String) return "Fail2";
  return "OK";
}
""")
external fun jsCheckPrimitiveString(s: String): String

fun box(): String {
    val tricky = "🚀\u0000e\u0301"
    val r = jsCheckPrimitiveString(tricky)
    return if (r == "OK") "OK" else "Fail3"
}
