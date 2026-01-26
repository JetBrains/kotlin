
// FILE: enums.kt
package e

enum class Mode { A, B }

fun select(m: Mode = Mode.B): String =
    when (m) {
        Mode.A -> "A"
        Mode.B -> "B"
    }

// FILE: test.kt
import e.Mode
import e.select

fun box(): String {
	if (select(Mode.A) != "A") return "FAIL1"
	if (select() != "B") return "FAIL2"
	return "OK"
}

