// RUN_PIPELINE_TILL: BACKEND
// KT-174 Nullability info for extension function receivers
interface Tree {}

fun Any?.TreeValue() : Tree {
  if (this is Tree) return this
    throw Exception()
}
