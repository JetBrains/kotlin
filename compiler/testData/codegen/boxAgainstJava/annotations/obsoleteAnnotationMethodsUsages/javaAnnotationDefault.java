import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface JavaAnn {
    String value() default "default";
}

@Retention(RetentionPolicy.RUNTIME)
@interface JavaAnn2 {
    int a() default 1;
    byte b() default 1;
    short c() default 1;
    double d() default 1;
    float e() default 1;
    long j() default 1;
    String f() default "default";
}