// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
class TextMateScope(
  val scopeName: CharSequence?,
  val parent: TextMateScope?,
) {
  companion object {
    @JvmField
    val EMPTY: TextMateScope = TextMateScope(null, null)
    @JvmField
    val WHITESPACE: TextMateScope = EMPTY.add("token.whitespace")
  }

  val level: Int = parent?.level?.inc() ?: 0
  val dotsCount: Int = (scopeName?.count { it == '.' } ?: 0) + (parent?.dotsCount ?: 0)
  val isEmpty: Boolean = (parent == null || parent.isEmpty) && (scopeName == null || scopeName.isEmpty())

  private val hashCode: Int = arrayOf(scopeName, parent).contentHashCode()

  fun add(scopeName: CharSequence?): TextMateScope {
    return TextMateScope(scopeName, this)
  }

  override fun toString(): String {
    return buildString {
      if (scopeName != null) {
        append(scopeName)
      }
      generateSequence(parent, TextMateScope::parent)
        .mapNotNull(TextMateScope::scopeName)
        .forEach { parentScopeName ->
          insert(0, "$parentScopeName ")
        }
    }.trim()
  }


  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    other as TextMateScope
    return level == other.level && hashCode == other.hashCode && scopeName == other.scopeName
  }

  override fun hashCode(): Int {
    return hashCode
  }
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, asExpression, callableReference, classDeclaration,
companionObject, disjunctionExpression, elvisExpression, equalityExpression, flexibleType, functionDeclaration,
ifExpression, integerLiteral, javaFunction, lambdaLiteral, nullableType, objectDeclaration, operator, override,
primaryConstructor, propertyDeclaration, safeCall, smartcast, stringLiteral, thisExpression */
