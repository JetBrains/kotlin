// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK
// MODULE: lib
// FILE: Java.java

public class Java {
    public Java(String s) {}

    void foo(long l) {}

    class Inner {}
}

// FILE: MyEnum.java

import java.util.concurrent.TimeUnit;
import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@interface Annotation {}

public enum MyEnum {
    A(TimeUnit.SECONDS);
    // The annotation is to make sure a Java 8 bug does not affect us
    MyEnum(@Annotation TimeUnit a) { }
}

// MODULE: main(lib)
// FILE: 1.kt

import java.lang.reflect.Parameter
import kotlin.test.*
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*

class Kotlin(val x: Int) {
    fun withInlineClass(z: InlineClass) {}
    suspend fun withContinuation() {}
    fun withDefault(d: Long = 0) {}

    inner class Inner
}

@JvmInline
value class InlineClass(val y: UInt)

class WithInlineClass(val z: InlineClass)

class WithDefault(val d: Long = 0L)

fun checkKotlinToJava(kotlinParameter: KParameter, javaParameter: Parameter) {
    assertEquals(javaParameter, kotlinParameter.javaParameter)
}

fun box(): String {
    // Instance parameters don't exist in Java (except for inner class constructors)
    for (callable in listOf(Java::foo, Kotlin::withInlineClass, Kotlin::withContinuation, Kotlin::withDefault)) {
        if (callable.instanceParameter!!.javaParameter != null) return "Fail: Java parameter was returned for an instance parameter"
    }

    checkKotlinToJava(::Java.valueParameters.single(), Java::class.java.constructors.single().parameters[0])
    checkKotlinToJava(Java::foo.valueParameters.single(), Java::foo.javaMethod!!.parameters[0])

    checkKotlinToJava(Kotlin::withInlineClass.valueParameters.single(), Kotlin::withInlineClass.javaMethod!!.parameters[0])
    checkKotlinToJava(::WithInlineClass.valueParameters.single(), ::WithInlineClass.javaConstructor!!.parameters[0])

    checkKotlinToJava(Kotlin::withDefault.valueParameters.single(), Kotlin::withDefault.javaMethod!!.parameters[0])
    checkKotlinToJava(::WithDefault.valueParameters.single(), ::WithDefault.javaConstructor!!.parameters[0])

    // Inline class constructors point to a Java static method
    checkKotlinToJava(::InlineClass.valueParameters.single(), InlineClass::class.java.getDeclaredMethod("constructor-impl", Int::class.java).parameters[0])

    // Inner class constructor's instance parameter
    // The JDK 8 bug described in (internal) 'ReflectKParameter.javaParameter' does not affect 'Method.getParameters'
    checkKotlinToJava(Java::Inner.instanceParameter!!, Java::Inner.javaConstructor!!.parameters[0])
    assertEquals(Java::class.java, Java::Inner.javaConstructor!!.parameters[0].type)

    checkKotlinToJava(Kotlin::Inner.instanceParameter!!, Kotlin::Inner.javaConstructor!!.parameters[0])

    // Enum constructors have 2 implicit parameters (name/ordinal) not present in kotlin-reflect
    // The JDK <17 bug described in (internal) 'ReflectKParameter.javaParameter' does not affect 'Method.getParameters' either
    checkKotlinToJava(MyEnum::class.constructors.single().parameters[0], MyEnum::class.java.declaredConstructors.single().parameters[2])

    return "OK"
}
