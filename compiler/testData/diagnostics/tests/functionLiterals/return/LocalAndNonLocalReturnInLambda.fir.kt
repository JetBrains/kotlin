// !CHECK_TYPE

fun test2(a: Int) {
    val x = run f@{
      if (a > 0) return
      return@f 1
    }
    checkSubtype<Int>(x)
}

fun <T> run(f: () -> T): T { return f() }