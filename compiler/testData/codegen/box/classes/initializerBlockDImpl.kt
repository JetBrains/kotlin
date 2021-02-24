// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
class World() {
  public val items: ArrayList<Item> = ArrayList<Item>()

  inner class Item() {
    init {
      items.add(this)
    }
  }

  val foo = Item()
}

fun box() : String {
  val w = World()
  if (w.items.size != 1) return "fail"
  return "OK"
}
