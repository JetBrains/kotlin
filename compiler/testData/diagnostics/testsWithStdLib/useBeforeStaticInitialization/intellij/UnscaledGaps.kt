// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
interface UnscaledGaps {
  companion object {
    val EMPTY: UnscaledGaps = EmptyGaps
  }

  val top: Int
  val left: Int
  val bottom: Int
  val right: Int

  val width: Int
    get() = left + right

  val height: Int
    get() = top + bottom
}

private object EmptyGaps : UnscaledGaps {
    override val top: Int = 0
    override val left: Int = 0
    override val bottom: Int = 0
    override val right: Int = 0
}

/* GENERATED_FIR_TAGS: additiveExpression, companionObject, getter, integerLiteral, interfaceDeclaration,
objectDeclaration, override, propertyDeclaration */
