// TARGET_BACKEND: WASM
// WITH_STDLIB

@JsFun("""
(s) => {
  const nulIndex = s.indexOf("\u0000");
  const hasEmoji = s.includes("🚀");
  const splitNulLen = s.split("\u0000").length;
  const lastChar = s.at(-1);

  if (nulIndex !== 3) return "Fail1";
  if (!hasEmoji) return "Fail2";
  if (splitNulLen !== 2) return "Fail3";
  if (lastChar !== "Z") return "Fail4";

  return "OK";
}
""")
external fun jsCheckStringApis(s: String): String

fun box(): String {
    val s = "A🚀\u0000e\u0301Z"
    return jsCheckStringApis(s)
}
