// KT-174 Nullability info for extension function receivers
interface Tree {}

fun Any?.TreeValue() : Tree {
  if (this is Tree) return <!DEBUG_INFO_SMARTCAST!>this<!>
    throw Exception()
}
