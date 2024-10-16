// FIR_IDENTICAL
// ISSUE: KT-72178
// FULL_JDK
// WITH_STDLIB

import java.util.EnumMap

enum class Key
class Value

val map = EnumMap<_, Value>(Key::class.java)
