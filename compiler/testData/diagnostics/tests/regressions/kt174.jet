// KT-174 Nullability info for extension function receivers
trait Tree {}

fun Any?.TreeValue() : Tree {
  if (this is Tree) return this
    throw Exception()
}
