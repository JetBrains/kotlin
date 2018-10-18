// RUNTIME_WITH_FULL_JDK
val s = Sam<caret> { d, t ->
    val s = "$d$t"
    listOf(s)
}