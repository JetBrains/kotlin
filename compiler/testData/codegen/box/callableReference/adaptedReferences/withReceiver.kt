// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

fun MutableSet<CharSequence>.foo(s: CharSequence): Set<CharSequence> {
    s.also(::add)
    return this
}

fun box(): String = mutableSetOf<CharSequence>().foo("OK").single() as String
