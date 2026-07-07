// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB

sealed class Attributes() {
    inner class Required internal constructor() : Entity {}

    inner class Optional internal constructor() : Entity {}

    inner class Many internal constructor() : Entity {}

    protected fun optionalValue(): Optional = Optional()

    protected fun optionalTransient(): Optional = Optional()

    protected fun requiredRef(): Required = Required()

    protected fun requiredValue(): Required = Required()

    protected fun manyRef(): Many = Many()
}

interface Entity {

  companion object : Attributes() {
    /**
     * A reference to the corresponding [EntityType] entity
     * */
    val Type = requiredRef()

    /**
     * The attribute storing the instance, returned by [EntityType.reify], of a user-defined implementation of [Entity] interface.
     * An entity might exist without [EntityObject] if user code defining the corresponding [EntityType] is not loaded.
     * e.g. entity is restored from a snapshot, or received over the wire.
     * */
    val EntityObject = optionalTransient()

    /**
     * An arbitrary indexed value associated with an entity, it is used to track all entities contributed by plugins to clean them up.
     * */
    val Module = optionalValue()
  }
}

data class DB(
  val index: Int,
  val queryCache: Int,
) {

  companion object {
    private val EMPTY = lazy {
      DB(
        index = 0,
        queryCache = 0
      ).apply { Entity }
    }

    @JvmStatic
    fun empty(): DB = EMPTY.value
  }
}

interface DocumentComponentEntity : Entity {
  val document: Entity
    get() = DocumentAttr

  companion object : Attributes() {
    val DocumentAttr: Required = requiredRef()
  }
}

abstract class EntityType() : Attributes(), Entity {
  companion object : EntityType() {
    val Ident = requiredValue()

    val PossibleAttributes = manyRef()

    val Name = requiredValue()
  }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, data, functionDeclaration, getter, inner, integerLiteral,
interfaceDeclaration, lambdaLiteral, objectDeclaration, primaryConstructor, propertyDeclaration, sealed */
