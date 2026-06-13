// FILE: Hidden.kt
class Hidden { private class Nested }

// FILE: main.kt
val x: Hidden.Nested = null!!
