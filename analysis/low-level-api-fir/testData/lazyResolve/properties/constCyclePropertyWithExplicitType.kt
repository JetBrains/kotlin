const val with<caret>Cycle1: Int = 1 + withCycle2
const val withCycle2: Int = 2 + withCycle3
const val withCycle3: Int = 3 + withCycle1