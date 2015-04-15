import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface JavaAnn {
    Class<?>[] value();
}

class O {}
class K {}

@JavaAnn({O.class, K.class})
class MyJavaClass {}
