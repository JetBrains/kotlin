// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JVM_IR
// WITH_REFLECT
// FILE: main.kt

// See compiler/testData/diagnostics/tests/multiplatform/defaultArguments/annotationsViaActualTypeAlias2.kt

// This test checks the same behavior but against the Java implementation compiled to the .class file (as opposed to a .java source file).
// Enum annotation argument is commented below, because to be able to resolve E in Jnno.java we have to have a multi-module test where
// one of the modules also contains Java files, and that is too complicated for our test infrastructure at the moment.

import kotlin.reflect.KClass

expect annotation class Anno(
    val b: Byte = 1.toByte(),
    val c: Char = 'x',
    val d: Double = 3.14,
    val f: Float = -2.72f,
    val i: Int = 42424242,
    val i2: Int = 53535353,
    val j: Long = 239239239239239L,
    val j2: Long = 239239L,
    val s: Short = 42.toShort(),
    val z: Boolean = true,
    val ba: ByteArray = [(-1).toByte()],
    val ca: CharArray = ['y'],
    val da: DoubleArray = [-3.14159],
    val fa: FloatArray = [2.7218f],
    val ia: IntArray = [424242],
    val ja: LongArray = [239239239239L, 239239L],
    val sa: ShortArray = [(-43).toShort()],
    val za: BooleanArray = [false, true],
    val str: String = "fizz",
    val k: KClass<*> = Number::class,
    // val e: E = E.E1,
    // TODO: val a: A = A("1"),
    val stra: Array<String> = ["bu", "zz"],
    val ka: Array<KClass<*>> = [Double::class, String::class, LongArray::class, Array<Array<Array<Int>>>::class, Unit::class]
    // val ea: Array<E> = [E.E2, E.E3],
    // TODO: val aa: Array<A> = [A("2"), A("3")]
)

// enum class E { E1, E2, E3 }

annotation class A(val value: String)

@Anno
fun test() {}

actual typealias Anno = Jnno

fun box(): String {
    // We don't need to check the contents, just check that there are no anomalities in the bytecode by loading annotations
    ::test.annotations.toString()

    return "OK"
}

// FILE: Jnno.java

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Jnno {
    byte b() default 1;
    char c() default 'x';
    double d() default 3.14;
    float f() default -2.72f;
    int i() default 42424242;
    int i2() default 21212121 + 32323232;
    long j() default 239239239239239L;
    long j2() default 239239;
    short s() default 42;
    boolean z() default true;
    byte[] ba() default {-1};
    char[] ca() default {'y'};
    double[] da() default {-3.14159};
    float[] fa() default {2.7218f};
    int[] ia() default {424242};
    long[] ja() default {239239239239L, 239239};
    short[] sa() default {-43};
    boolean[] za() default {false, true};
    String str() default "fi" + "zz";
    Class<?> k() default Number.class;
    // E e() default E.E1;
    // TODO: A a() default @A("1");
    String[] stra() default {"bu", "zz"};
    Class<?>[] ka() default {double.class, String.class, long[].class, Integer[][][].class, void.class};
    // E[] ea() default {E.E2, E.E3};
    // TODO: A[] aa() default {@A("2"), @A("3")};
}
