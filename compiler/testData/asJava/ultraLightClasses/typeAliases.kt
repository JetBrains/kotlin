typealias JO = JvmOverloads

/** should load cls */
object O {
  @JO fun foo(a: Int = 1, b: String = "") {}
}
