// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_TEXT

// FILE: JavaAbstractVal.java
public abstract value class JavaAbstractVal {}

// FILE: test.kt
import java.time.ZoneId
import java.time.ZoneOffset

abstract value class AbstractVal
sealed value class SealedVal
value class SealedChild(val x: Int) : SealedVal()

// Abstract value classes, `java.lang.Record` and `java.lang.Number` among them.
class AbstractHolder(val a: AbstractVal?, val s: SealedVal?, val j: JavaAbstractVal?, val r: Record?, val n: Number?)

// Value-based JDK classes that are not value classes.
class JdkIdentityHolder(val v: Runtime.Version?, val z: ZoneId?, val o: ZoneOffset?)

fun box(): String {
    val a = AbstractHolder(null, SealedChild(1), null, null, 1)
    if (a.s != SealedChild(1) || a.n != 1) return "AbstractHolder: ${a.s}, ${a.n}"
    val j = JdkIdentityHolder(Runtime.version(), ZoneId.of("UTC"), ZoneOffset.UTC)
    if (j.o != ZoneOffset.UTC) return "JdkIdentityHolder: ${j.o}"
    return "OK"
}

// 2 ATTRIBUTE LoadableDescriptors
// 1 ATTRIBUTE LoadableDescriptors : LAbstractVal;, LSealedVal;, LJavaAbstractVal;, Ljava/lang/Record;\n
// 1 ATTRIBUTE LoadableDescriptors : Ljava/lang/Runtime\$Version;, Ljava/time/ZoneId;, Ljava/time/ZoneOffset;\n
