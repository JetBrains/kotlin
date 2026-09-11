// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.*

@JvmInline value class UserId(val raw: Int)
@JvmInline value class UserName(val raw: String)

data class UserRecord(val id: UserId, val name: UserName, val active: Boolean = true)

class UserService {
    fun createUser(id: UserId, name: UserName): UserRecord = UserRecord(id, name)
    fun findById(id: UserId): UserRecord? = if (id.raw > 0) UserRecord(id, UserName("found")) else null
    fun withDefaultActive(id: UserId, name: UserName, active: Boolean = true): UserRecord =
        UserRecord(id, name, active)
}

fun box(): String {
    // Constructor of data class with value class parameters via callBy
    val ctor = UserRecord::class.primaryConstructor
    assertNotNull(ctor)
    assertEquals(3, ctor.parameters.size)
    assertTrue(ctor.parameters[2].isOptional) // active has default

    // callBy with required args only (default for active)
    val user1 = ctor.callBy(mapOf(
        ctor.parameters[0] to UserId(1),
        ctor.parameters[1] to UserName("Alice")
    ))
    assertEquals(UserId(1), user1.id)
    assertEquals(UserName("Alice"), user1.name)
    assertTrue(user1.active) // default

    // callBy with all args
    val user2 = ctor.callBy(mapOf(
        ctor.parameters[0] to UserId(2),
        ctor.parameters[1] to UserName("Bob"),
        ctor.parameters[2] to false
    ))
    assertFalse(user2.active)

    // Member functions with value class params via callBy
    val service = UserService()
    val createFn = UserService::class.memberFunctions.single { it.name == "createUser" }
    val created = createFn.callBy(mapOf(
        createFn.instanceParameter!! to service,
        createFn.valueParameters[0] to UserId(10),
        createFn.valueParameters[1] to UserName("Carol")
    ))
    assertEquals(UserId(10), created.id)

    val withDefaultFn = UserService::class.memberFunctions.single { it.name == "withDefaultActive" }
    assertTrue(withDefaultFn.valueParameters[2].isOptional)
    val withDefault = withDefaultFn.callBy(mapOf(
        withDefaultFn.instanceParameter!! to service,
        withDefaultFn.valueParameters[0] to UserId(5),
        withDefaultFn.valueParameters[1] to UserName("Dave")
    ))
    assertTrue(withDefault.active) // default active=true

    // Value class parameters: their return type in reflection
    val idParam = createFn.valueParameters[0]
    assertEquals("UserId", (idParam.type.classifier as KClass<*>).simpleName)
    assertTrue((idParam.type.classifier as KClass<*>).isValue)

    // findById returns nullable
    val findFn = UserService::class.memberFunctions.single { it.name == "findById" }
    assertTrue(findFn.returnType.isMarkedNullable)

    val found = findFn.callBy(mapOf(
        findFn.instanceParameter!! to service,
        findFn.valueParameters[0] to UserId(1)
    ))
    assertNotNull(found)

    val notFound = findFn.callBy(mapOf(
        findFn.instanceParameter!! to service,
        findFn.valueParameters[0] to UserId(0)
    ))
    assertNull(notFound)

    return "OK"
}
