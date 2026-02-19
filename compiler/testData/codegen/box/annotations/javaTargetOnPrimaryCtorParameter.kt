// WITH_STDLIB
// WITH_REFLECT
// TARGET_BACKEND: JVM_IR
// FIR_DUMP
// DUMP_IR

// FILE: NoTarget.java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface NoTarget {
}

// FILE: PropValueField.java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface PropValueField {
}

// FILE: ParameterOnly.java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface ParameterOnly {
}

// FILE: FieldOnly.java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface FieldOnly {
}

// FILE: test.kt
import kotlin.reflect.full.declaredMemberProperties

class Foo(
    @NoTarget
    @PropValueField
    @ParameterOnly
    @FieldOnly
    var param: Int
)

fun box(): String {
    val clazz = Foo::class

    val parameterAnnotations = clazz.constructors.single().parameters.single().annotations.map { it.annotationClass.simpleName ?: "" }.toSet()
    val fieldAnnotations = Foo::class.java.getDeclaredField("param").annotations.map { it.annotationClass.simpleName ?: "" }.toSet()

    if (parameterAnnotations != setOf("NoTarget", "PropValueField", "ParameterOnly")) return "Parameters:" + parameterAnnotations
    if (fieldAnnotations != setOf("FieldOnly")) return "Field:" + fieldAnnotations

    return "OK"
}
