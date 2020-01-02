// !CHECK_TYPE

fun test(a: Int) {
    val x = run f@{
      if (a > 0) return@f
      else return@f Unit
    }
    checkSubtype<Unit>(x)
}
