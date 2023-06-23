// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR
// StackOverflow with StringBuilder (escape()) on Android 4.4
// IGNORE_BACKEND: ANDROID

fun escapeChar(c : Char) : String? = when (c) {
  '\\' -> "\\\\"
  '\n' -> "\\n"
  '"'  -> "\\\""
  else -> "" + c
}

fun String.escape(i : Int = 0, result : String = "") : String =
  if (i == length) result
  else escape(i + 1, result + escapeChar(get(i)))

fun box() : String {
  val s = "  System.out?.println(\"fun escapeChar(c : Char) : String? = when (c) {\");\n  System.out?.println(\"  '\\\\\\\\' => \\\"\\\\\\\\\\\\\\\\\\\"\");\n  System.out?.println(\"  '\\\\n' => \\\"\\\\\\\\n\\\"\");\n  System.out?.println(\"  '\\\"'  => \\\"\\\\\\\\\\\\\\\"\\\"\");\n  System.out?.println(\"  else => String.valueOf(c)\");\n  System.out?.println(\"}\");\n  System.out?.println();\n  System.out?.println(\"fun String.escape(i : Int = 0, result : String = \\\"\\\") : String =\");\n  System.out?.println(\"  if (i == length) result\");\n  System.out?.println(\"  else escape(i + 1, result + escapeChar(this.get(i)))\");\n  System.out?.println();\n  System.out?.println(\"fun main(args : Array<String>) {\");\n  System.out?.println(\"  val s = \\\"\" + s.escape() + \"\\\";\");\n  System.out?.println(s);\n}\n";
  System.out?.println("fun escapeChar(c : Char) : String? = when (c) {");
  System.out?.println("  '\\\\' => \"\\\\\\\\\"");
  System.out?.println("  '\\n' => \"\\\\n\"");
  System.out?.println("  '\"'  => \"\\\\\\\"\"");
  System.out?.println("  else => String.valueOf(c)");
  System.out?.println("}");
  System.out?.println();
  System.out?.println("fun String.escape(i : Int = 0, result : String = \"\") : String =");
  System.out?.println("  if (i == length) result");
  System.out?.println("  else escape(i + 1, result + escapeChar(this.get(i)))");
  System.out?.println();
  System.out?.println("fun main(args : Array<String>) {");
  System.out?.println("  val s = \"" + s.escape() + "\";");
  System.out?.println(s);
  return "OK"
}
