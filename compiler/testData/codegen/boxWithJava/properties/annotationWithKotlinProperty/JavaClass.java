import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class JavaClass {

    @Retention(RetentionPolicy.RUNTIME)
    @interface Foo {
        int value();
    }

    @Foo(KotlinClass.FOO_INT)
    public String test() throws NoSuchMethodException {
        return KotlinClass.FOO_STRING +
               JavaClass.class.getMethod("test").getAnnotation(Foo.class).value();
    }
}