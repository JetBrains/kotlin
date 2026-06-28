// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
interface OperationExecutionId {

  /**
   * Execution context allows to forward data through execution events.
   */
  val executionContext: Any

  companion object {

    val NONE: OperationExecutionId = createId("NONE")

    fun createId(
      debugName: String? = null
    ): OperationExecutionId {
      return object : OperationExecutionId {
        override val executionContext = "test"
        override fun toString() = debugName ?: "UNKNOWN"
      }
    }
  }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, companionObject, elvisExpression, functionDeclaration,
interfaceDeclaration, nullableType, objectDeclaration, override, propertyDeclaration, stringLiteral */
