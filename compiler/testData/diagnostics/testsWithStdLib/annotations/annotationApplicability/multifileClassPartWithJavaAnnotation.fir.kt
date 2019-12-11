// FILE: test.kt
@file:JvmName("MultifileClass")
@file:JvmMultifileClass
@file:JavaAnn
@file:JavaClassAnn
@file:JavaSourceAnn

// FILE: JavaAnn.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JavaAnn {
}

// FILE: JavaClassAnn.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface JavaClassAnn {
}

// FILE: JavaSourceAnn.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface JavaSourceAnn {
}

