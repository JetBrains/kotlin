import lib.Dup

fun common() = Dup().ok()

// The common fragment accepts libB only, but libA is earlier on the shared classpath.