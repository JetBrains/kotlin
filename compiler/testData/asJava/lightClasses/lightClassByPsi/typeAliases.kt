// LIBRARY_PLATFORMS: JVM

typealias JO = JvmOverloads

object O {
  @JO fun foo(a: Int = 1, b: String = "") {}
}


// DECLARATIONS_NO_LIGHT_ELEMENTS: TypeAliasesKt.class[JO]
// LIGHT_ELEMENTS_NO_DECLARATION: O.class[O]