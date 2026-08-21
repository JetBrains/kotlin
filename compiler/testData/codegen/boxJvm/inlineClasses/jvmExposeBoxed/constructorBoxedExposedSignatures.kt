// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// FULL_JDK
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ParamAnn

@JvmInline
value class StringWrapper @JvmExposeBoxed constructor(val s: String?)

@JvmExposeBoxed
class Test(@ParamAnn val s: StringWrapper?)

// FILE: Main.java
public class Main {
    public static Test create(StringWrapper s) {
        return new Test(s);
    }
}

// FILE: Box.kt
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier

private fun constructorOrNull(vararg types: Class<*>): Constructor<Test>? =
    try {
        Test::class.java.getDeclaredConstructor(*types)
    } catch (e: NoSuchMethodException) {
        null
    }

fun box(): String {
    if (Main.create(StringWrapper("OK")).s?.s != "OK") return "FAIL: call from Java"

    if (Test(StringWrapper("OK")).s?.s != "OK") return "FAIL: call from Kotlin"

    val marker = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker")

    val exposed = constructorOrNull(StringWrapper::class.java)
        ?: return "FAIL: no <init>(LStringWrapper;)V"
    if (!Modifier.isPublic(exposed.modifiers)) return "FAIL: exposed constructor is not public"
    if (exposed.isSynthetic) return "FAIL: exposed constructor is synthetic"

    val accessor = constructorOrNull(StringWrapper::class.java, marker)
        ?: return "FAIL: no <init>(LStringWrapper;Lkotlin/jvm/internal/DefaultConstructorMarker;)V"
    if (!accessor.isSynthetic) return "FAIL: accessor is not synthetic"

    val all = Test::class.java.declaredConstructors
    if (all.size != 2) return "FAIL: expected exactly 2 constructors, got " + all.joinToString { it.toString() }

    return "OK"
}
