// WITH_RUNTIME

interface I

private fun f() = <caret>listOf(object : I { })
