// COMPILER_ARGUMENTS: -XXLanguage:+NewInference

val s = Sam<caret> { b ->
    if (b) return@Sam "x"
    "y"
}