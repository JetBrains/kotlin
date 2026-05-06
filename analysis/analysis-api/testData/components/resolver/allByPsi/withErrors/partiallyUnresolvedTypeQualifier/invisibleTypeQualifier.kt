// FILE: Hidden.kt
private class Hidden { private class Nested }

// FILE: main.kt
val x: Hidden.Nested = null!!
