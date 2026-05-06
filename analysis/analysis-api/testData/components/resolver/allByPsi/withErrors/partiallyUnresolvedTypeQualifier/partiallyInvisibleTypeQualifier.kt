// FILE: Hidden.kt
private class Hidden { class Nested }

// FILE: main.kt
val x: Hidden.Nested = null!!
