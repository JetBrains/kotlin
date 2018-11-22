typealias JO = JvmOverloads

object O {
  @JO fun foo(a: Int = 1, b: String = "") {}
}
