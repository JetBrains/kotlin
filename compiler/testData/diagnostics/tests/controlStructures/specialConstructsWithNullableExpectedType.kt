val ns: String? = null

val testElvis1: String? = ns ?: ""
val testElvis2: String = run { ns ?: "" }
val testElvis3: String? = run { ns ?: "" }

val testIf1: String? = if (true) "" else ""
val testIf2: String? = run { if (true) "" else "" }
val testIf3: String? = if (true) run { "" } else ""
val testIf4: String? = run { run { if (true) "" else "" } }
val testIf5: String? = run { if (true) run { "" } else "" }

val testWhen1: String? = when { else -> "" }
val testWhen2: String? = run { when { else -> "" } }
val testWhen3: String? = when { else -> run { "" } }
val testWhen4: String? = run { run { when { else -> "" } } }
val testWhen5: String? = run { when { else -> run { "" } } }

val testExcl1: String? = run { ns!! }
val testExcl2: String? = run { run { ns!! } }