import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface JavaAnn {
    Class<?>[] args();
}

class O {}
class K {}

@JavaAnn(args = {O.class, K.class})
class MyJavaClass {}
