// TARGET_BACKEND: WASM
// WITH_STDLIB

@JsFun("""() => String.fromCharCode(0xD83D, 0xDE80)""")
external fun jsFromCharCodeRocket(): String

@JsFun("""() => `a\u0000b`""")
external fun jsTemplateWithNul(): String

@JsFun("""
() => {
  const boxed = new String("OK");
  if (typeof boxed !== "object") return "Fail1";
  if (!(boxed instanceof String)) return "Fail2";
  const unboxed = String(boxed);
  if (typeof unboxed !== "string") return "Fail3";
  return unboxed;
}
""")
external fun jsMakeAndUnboxStringObject(): String

fun box(): String {
    val rocket = jsFromCharCodeRocket()
    if (rocket != "🚀") return "Fail4"

    val nul = jsTemplateWithNul()
    if (nul != "a\u0000b") return "Fail5"

    val unboxed = jsMakeAndUnboxStringObject()
    if (unboxed != "OK") return "Fail6"

    return "OK"
}
