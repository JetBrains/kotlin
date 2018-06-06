// FILE: JavaAnn.java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface JavaAnn {
    String value() default "";
    int i() default 0;
}

// FILE: javaAnnotation.kt
@JavaAnn fun test1() {}

@JavaAnn(value="abc", i=123) fun test2() {}

@JavaAnn(i=123, value="abc") fun test3() {}
