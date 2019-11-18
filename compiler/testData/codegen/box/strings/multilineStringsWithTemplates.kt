// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {
    val s = "abc"
    val test1 = """$s"""
    if (test1 != "abc") return "Fail 1: $test1"

    val test2 = """${s}"""
    if (test2 != "abc") return "Fail 2: $test2"

    val test3 = """ "$s" """
    if (test3 != " \"abc\" ") return "Fail 3: $test3"

    val test4 = """ "${s}" """
    if (test4 != " \"abc\" ") return "Fail 4: $test4"

    val test5 =
"""
  ${s.length}
"""
    if (test5 != "\n  3\n") return "Fail 5: $test5"

    val test6 = """\n"""
    if (test6 != "\\n") return "Fail 6: $test6"

    val test7 = """\${'$'}foo"""
    if (test7 != "\\\$foo") return "Fail 7: $test7"

    val test8 = """$ foo"""
    if (test8 != "$ foo") return "Fail 8: $test8"

    return "OK"
}
