// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
class ExtendableHTMLViewFactory internal constructor(val exts: List<String>) {

  companion object {
    @JvmField
    internal val DEFAULT_EXTENSIONS: List<String> = listOf("A", "B", "C")

    @JvmField
    internal val DEFAULT: ExtendableHTMLViewFactory = ExtendableHTMLViewFactory(DEFAULT_EXTENSIONS)

    private val DEFAULT_EXTENSIONS_WORD_WRAP = DEFAULT_EXTENSIONS + "D"

    @JvmField
    internal val DEFAULT_WORD_WRAP: ExtendableHTMLViewFactory = ExtendableHTMLViewFactory(DEFAULT_EXTENSIONS_WORD_WRAP)
  }
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, companionObject, objectDeclaration, primaryConstructor,
propertyDeclaration, stringLiteral */
