// TARGET_BACKEND: JVM
// WITH_REFLECT
// ISSUE: KT-85831

// FILE: ObservableValue.java
public interface ObservableValue<T> {
    default Object isEqualTo(T value) {
        return null;
    }
}

// FILE: ObservableDouble.java
public interface ObservableDouble extends ObservableValue<Double> {
    Object isEqualTo(double value);
}

// FILE: OtherObservableDouble.java
public interface OtherObservableDouble extends ObservableValue<Double> {
}

// FILE: DoubleExpression.java
public abstract class DoubleExpression implements ObservableDouble, OtherObservableDouble {
    @Override
    public Object isEqualTo(double value) {
        return null;
    }
}

// FILE: javax/annotation/Nonnull.java
package javax.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Nonnull {}

// FILE: A.java
import javax.annotation.Nonnull;

public interface A {
    void foo(@Nonnull String s);
}

// FILE: B.java
import javax.annotation.Nonnull;

public interface B {
    void foo(@Nonnull String s);
}

// FILE: C.java
public abstract class C implements A, B {
}

// FILE: box.kt
import kotlin.reflect.KClass
import kotlin.test.assertEquals

fun KClass<*>.render(name: String): List<String> =
    members.filter { it.name == name }.map { it.toString() }.sorted()

fun box(): String {
    // The intersection override of `isEqualTo(Double!)` (inherited via ObservableDouble and OtherObservableDouble) must not be enhanced
    // from `isEqualTo(double)`, which has an equal Kotlin signature modulo flexibility.
    assertEquals(
        listOf(
            "fun DoubleExpression.isEqualTo(kotlin.Double!): kotlin.Any!",
            "fun DoubleExpression.isEqualTo(kotlin.Double): kotlin.Any!",
        ),
        DoubleExpression::class.render("isEqualTo"),
    )

    // The intersection override of two already enhanced functions must keep their nullability.
    assertEquals(listOf("fun C.foo(kotlin.String): kotlin.Unit"), C::class.render("foo"))

    return "OK"
}
