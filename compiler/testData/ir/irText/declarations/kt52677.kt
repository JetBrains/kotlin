// FIR_IDENTICAL
// ISSUE: KT-52677
// IGNORE_BACKEND: JS_IR

// KT-61141: throws kotlin.RuntimeException instead of java.lang.RuntimeException
// IGNORE_BACKEND: NATIVE

@Target(AnnotationTarget.TYPE)
annotation class MySerializable(val c: kotlin.reflect.KClass<*>)

public data class LoginSuccessPacket(val id: Uuid)

public typealias Uuid = @MySerializable(UuidSerializer::class) Uuid1

interface MySerializer<T>
public object UuidSerializer : MySerializer<Uuid>
public class Uuid1

fun foo(): Uuid { throw RuntimeException() }

fun bar() = foo()
