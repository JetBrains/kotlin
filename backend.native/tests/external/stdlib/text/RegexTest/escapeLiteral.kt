import kotlin.text.*

import kotlin.test.*


fun box() {
    val literal = """[-\/\\^$*+?.()|[\]{}]"""
    assertTrue(Regex.fromLiteral(literal).matches(literal))
    assertTrue(Regex.escape(literal).toRegex().matches(literal))
}
