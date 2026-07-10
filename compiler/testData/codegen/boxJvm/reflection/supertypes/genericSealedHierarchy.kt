// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.*

sealed class Either<out L, out R> {
    data class Left<out L>(val value: L) : Either<L, Nothing>()
    data class Right<out R>(val value: R) : Either<Nothing, R>()
    data object Empty : Either<Nothing, Nothing>()
}

sealed class Tree<out T> {
    data object Leaf : Tree<Nothing>()
    data class Node<out T>(val value: T, val left: Tree<T>, val right: Tree<T>) : Tree<T>()
}

fun box(): String {
    // Generic sealed class
    assertTrue(Either::class.isSealed)
    assertFalse(Either::class.isFinal)
    assertEquals(2, Either::class.typeParameters.size)
    assertEquals("L", Either::class.typeParameters[0].name)
    assertEquals(KVariance.OUT, Either::class.typeParameters[0].variance)
    assertEquals("R", Either::class.typeParameters[1].name)
    assertEquals(KVariance.OUT, Either::class.typeParameters[1].variance)

    // sealedSubclasses
    val eitherSubs = Either::class.sealedSubclasses
    assertEquals(3, eitherSubs.size)
    val names = eitherSubs.map { it.simpleName.orEmpty() }.sorted()
    assertEquals(listOf("Empty", "Left", "Right"), names)

    // Left implements Either<L, Nothing> — type arguments in supertype
    val leftSupertype = Either.Left::class.supertypes.firstOrNull { it.toString().contains("Either") }
    assertNotNull(leftSupertype, "Left should extend Either")
    assertEquals(2, leftSupertype.arguments.size)
    // First arg is L (type parameter), second is Nothing
    val secondArg = leftSupertype.arguments[1]
    assertEquals(KVariance.INVARIANT, secondArg.variance)
    assertTrue(secondArg.type?.toString()?.contains("Nothing") == true,
        "Second arg should be Nothing: ${secondArg.type}")

    // Right implements Either<Nothing, R>
    val rightSupertype = Either.Right::class.supertypes.firstOrNull { it.toString().contains("Either") }
    assertNotNull(rightSupertype)
    assertEquals(2, rightSupertype.arguments.size)
    assertTrue(rightSupertype.arguments[0].type?.toString()?.contains("Nothing") == true)

    // data object Empty implements Either<Nothing, Nothing>
    assertTrue(Either.Empty::class.isData)
    assertNotNull(Either.Empty::class.objectInstance)
    val emptySupertype = Either.Empty::class.supertypes.firstOrNull { it.toString().contains("Either") }
    assertNotNull(emptySupertype)
    assertTrue(emptySupertype.arguments.all { it.type?.toString()?.contains("Nothing") == true })

    // Tree sealed hierarchy
    assertTrue(Tree::class.isSealed)
    val treeSubs = Tree::class.sealedSubclasses
    assertEquals(2, treeSubs.size)

    // Tree.Leaf is data object
    val leaf = treeSubs.first { it.simpleName == "Leaf" }
    assertTrue(leaf.isData)
    assertNotNull(leaf.objectInstance)

    // Tree.Node has three constructor parameters (value, left, right)
    val node = treeSubs.first { it.simpleName == "Node" }
    assertTrue(node.isData)
    val nodeCtor = node.primaryConstructor
    assertNotNull(nodeCtor)
    assertEquals(3, nodeCtor.parameters.size)
    assertEquals("value", nodeCtor.parameters[0].name)
    assertEquals("left", nodeCtor.parameters[1].name)
    assertEquals("right", nodeCtor.parameters[2].name)

    // Node's left and right parameter types are Tree<T>
    val leftParamType = nodeCtor.parameters[1].type.toString()
    assertTrue(leftParamType.contains("Tree"), "Expected Tree in left param type: $leftParamType")

    // isSubclassOf for sealed hierarchy
    assertTrue(Either.Left::class.isSubclassOf(Either::class))
    assertTrue(Either.Right::class.isSubclassOf(Either::class))
    assertTrue(Either.Empty::class.isSubclassOf(Either::class))
    assertFalse(Either.Left::class.isSubclassOf(Either.Right::class))

    return "OK"
}
