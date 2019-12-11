// !CHECK_TYPE

interface A
interface B: A
interface C: A


fun test(a: C, b: B) {
    val x = run f@{
      if (a != b) return@f a
      b
    }
    checkSubtype<A>(x)
}
