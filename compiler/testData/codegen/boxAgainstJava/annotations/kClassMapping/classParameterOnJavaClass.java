import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface JavaAnn {
    Class<?> value();
}

class OK {}

@JavaAnn(OK.class)
class MyJavaClass {}
