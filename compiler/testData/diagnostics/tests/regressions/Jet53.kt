// FIR_IDENTICAL
// CHECK_TYPE

import java.util.Collections

val ab = checkSubtype<List<Int>?>(Collections.emptyList<Int>())
