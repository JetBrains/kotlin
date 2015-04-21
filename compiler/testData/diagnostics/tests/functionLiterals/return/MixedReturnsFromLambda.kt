// !CHECK_TYPE

trait A
trait B: A
trait C: A


fun test(a: C, b: B) {
    val x = run f@{
      if (a != b) return@f a
      b
    }
    checkSubtype<A>(x)
}

fun run<T>(f: () -> T): T { return f() }