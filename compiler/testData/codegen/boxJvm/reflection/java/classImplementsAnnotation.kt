// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/Nested.java
package test;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface Nested {
    String value();
}

// FILE: test/MyAnno.java
package test;

import java.lang.annotation.*;
import kotlin.annotation.AnnotationTarget;

@Retention(RetentionPolicy.RUNTIME)
public @interface MyAnno {
    byte b();
    char c();
    double d();
    float f();
    int i();
    long j();
    short s();
    boolean z();
    byte[] ba();
    char[] ca();
    double[] da();
    float[] fa();
    int[] ia();
    long[] ja();
    short[] sa();
    boolean[] za();
    String str();
    Class<?> k();
    Class<?> k2();
    AnnotationTarget e();
    Nested a();
    String[] stra();
    Class<?>[] ka();
    AnnotationTarget[] ea();
    Nested[] aa();
}

// FILE: test/NestedImpl.java
package test;

import java.lang.annotation.Annotation;

public class NestedImpl implements Nested {
    private final String value;

    public NestedImpl(String value) { this.value = value; }

    @Override public String value() { return value; }

    @Override public Class<? extends Annotation> annotationType() { return Nested.class; }
}

// FILE: test/AnnoImpl.java
package test;

import java.lang.annotation.Annotation;
import kotlin.Unit;
import kotlin.annotation.AnnotationTarget;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

public class AnnoImpl implements MyAnno {
    @Override public byte b() { return 1; }
    @Override public char c() { return 'x'; }
    @Override public double d() { return 3.14; }
    @Override public float f() { return -2.72f; }
    @Override public int i() { return 42424242; }
    @Override public long j() { return 239239239239239L; }
    @Override public short s() { return 42; }
    @Override public boolean z() { return true; }
    @Override public byte[] ba() { return new byte[] {-1}; }
    @Override public char[] ca() { return new char[] {'y'}; }
    @Override public double[] da() { return new double[] {-3.14159}; }
    @Override public float[] fa() { return new float[] {2.7218f}; }
    @Override public int[] ia() { return new int[] {424242}; }
    @Override public long[] ja() { return new long[] {239239239239L}; }
    @Override public short[] sa() { return new short[] {-43}; }
    @Override public boolean[] za() { return new boolean[] {false, true}; }
    @Override public String str() { return "lol"; }
    @Override public Class<?> k() { return Number.class; }
    @Override public Class<?> k2() { return int[].class; }
    @Override public AnnotationTarget e() { return AnnotationTarget.EXPRESSION; }
    @Override public Nested a() { return new NestedImpl("1"); }
    @Override public String[] stra() { return new String[] {"lmao"}; }
    @Override public Class<?>[] ka() { return new Class<?>[] {Double.class, Unit.class, long[].class, String[].class, Function0.class, Function1.class}; }
    @Override public AnnotationTarget[] ea() { return new AnnotationTarget[] {AnnotationTarget.TYPEALIAS, AnnotationTarget.FIELD}; }
    @Override public Nested[] aa() { return new Nested[] {new NestedImpl("2"), new NestedImpl("3")}; }

    @Override public Class<? extends Annotation> annotationType() { return MyAnno.class; }
}

// FILE: box.kt
import kotlin.reflect.full.memberProperties
import kotlin.test.assertEquals
import test.AnnoImpl

fun box(): String {
    val properties = AnnoImpl::class.memberProperties.associateBy { it.name }
    fun check(name: String, expected: String) = assertEquals(expected, properties.getValue(name).toString())
    check("b", "val test.AnnoImpl.b: kotlin.Byte")
    check("c", "val test.AnnoImpl.c: kotlin.Char")
    check("d", "val test.AnnoImpl.d: kotlin.Double")
    check("f", "val test.AnnoImpl.f: kotlin.Float")
    check("i", "val test.AnnoImpl.i: kotlin.Int")
    check("j", "val test.AnnoImpl.j: kotlin.Long")
    check("s", "val test.AnnoImpl.s: kotlin.Short")
    check("z", "val test.AnnoImpl.z: kotlin.Boolean")
    check("ba", "val test.AnnoImpl.ba: kotlin.ByteArray")
    check("ca", "val test.AnnoImpl.ca: kotlin.CharArray")
    check("da", "val test.AnnoImpl.da: kotlin.DoubleArray")
    check("fa", "val test.AnnoImpl.fa: kotlin.FloatArray")
    check("ia", "val test.AnnoImpl.ia: kotlin.IntArray")
    check("ja", "val test.AnnoImpl.ja: kotlin.LongArray")
    check("sa", "val test.AnnoImpl.sa: kotlin.ShortArray")
    check("za", "val test.AnnoImpl.za: kotlin.BooleanArray")
    check("str", "val test.AnnoImpl.str: kotlin.String")
    check("k", "val test.AnnoImpl.k: kotlin.reflect.KClass<*>")
    check("k2", "val test.AnnoImpl.k2: kotlin.reflect.KClass<*>")
    check("e", "val test.AnnoImpl.e: kotlin.annotation.AnnotationTarget")
    check("a", "val test.AnnoImpl.a: test.Nested")
    check("stra", "val test.AnnoImpl.stra: kotlin.Array<kotlin.String>")
    check("ka", "val test.AnnoImpl.ka: kotlin.Array<kotlin.reflect.KClass<*>>")
    check("ea", "val test.AnnoImpl.ea: kotlin.Array<kotlin.annotation.AnnotationTarget>")
    check("aa", "val test.AnnoImpl.aa: kotlin.Array<test.Nested>")
    return "OK"
}
