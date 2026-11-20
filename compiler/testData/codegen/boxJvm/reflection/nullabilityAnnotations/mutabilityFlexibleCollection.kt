// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK
// FILE: javax/annotation/Nullable.java
package javax.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Nullable {}

// FILE: javax/annotation/Nonnull.java
package javax.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Nonnull {}

// FILE: test/J.java
package test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class J {
    @Nullable
    public Collection<String> nullableCollection() { return null; }

    @Nonnull
    public List<String> notNullList() { return null; }

    public void nullableMapParameter(@Nullable Map<String, Integer> map) {}

    @Nullable
    public List<? super String> nullableListWithContravariantArgument() { return null; }
}

// FILE: test/K.kt
package test

interface K {
    var mutableList: MutableList<String>
    val readOnlyList: List<String>
}

// FILE: test/JK.java
package test;

import java.util.List;
import javax.annotation.Nonnull;

public abstract class JK implements K {
    @Override
    @Nonnull
    public List<String> getMutableList() { return null; }

    @Override
    @Nonnull
    public List<String> getReadOnlyList() { return null; }
}

// FILE: box.kt
package test

import java.lang.reflect.ParameterizedType
import kotlin.reflect.*
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

fun check(expected: String, type: KType, expectedClassifier: Class<*>) {
    assertEquals(expected, type.toString())
    assertEquals(expectedClassifier.kotlin, type.classifier)
    assertEquals(expectedClassifier, (type.javaType as ParameterizedType).rawType)
}

fun box(): String {
    check("kotlin.collections.(Mutable)Collection<kotlin.String!>?", J::nullableCollection.returnType, java.util.Collection::class.java)
    check("kotlin.collections.(Mutable)List<kotlin.String!>", J::notNullList.returnType, java.util.List::class.java)
    check("kotlin.collections.(Mutable)Map<kotlin.String!, kotlin.Int!>?", J::nullableMapParameter.parameters[1].type, java.util.Map::class.java)
    check("kotlin.collections.MutableList<in kotlin.String!>?", J::nullableListWithContravariantArgument.returnType, java.util.List::class.java)

    val mutableList = JK::class.members.single { it.name == "mutableList" } as KMutableProperty1<*, *>
    check("kotlin.collections.MutableList<kotlin.String>", mutableList.returnType, java.util.List::class.java)
    check("kotlin.collections.MutableList<kotlin.String>", mutableList.getter.returnType, java.util.List::class.java)
    check("kotlin.collections.MutableList<kotlin.String>", mutableList.setter.parameters[1].type, java.util.List::class.java)

    val readOnlyList = JK::class.members.single { it.name == "readOnlyList" } as KProperty1<*, *>
    check("kotlin.collections.List<kotlin.String>", readOnlyList.returnType, java.util.List::class.java)

    return "OK"
}
