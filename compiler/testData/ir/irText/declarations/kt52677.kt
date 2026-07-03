// ISSUE: KT-52677
// WITH_STDLIB
// DUMP_IR_DIFFERENCE: JVM
//   K/JVM throws java.lang.RuntimeException instead of kotlin.RuntimeException

@Target(AnnotationTarget.TYPE)
annotation class MySerializable(val c: kotlin.reflect.KClass<*>)

public data class LoginSuccessPacket(val id: Uuid)

public typealias Uuid = @MySerializable(UuidSerializer::class) Uuid1

interface MySerializer<T>
public object UuidSerializer : MySerializer<Uuid>
public class Uuid1

fun foo(): Uuid { throw RuntimeException() }

fun bar() = foo()
