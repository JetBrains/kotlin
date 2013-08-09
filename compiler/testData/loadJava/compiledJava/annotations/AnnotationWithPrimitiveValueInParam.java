package test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// TODO: remove comments when fix in idea will be accepted
public interface AnnotationWithPrimitiveValueInParam {

    public @interface Ann {
        int i();
        // TODO short s();
        // TODO byte b();
        long l();
        double d();
        float f();
        boolean bool();
        // TODO char c();
        String str();
    }

    @Ann(
            i = 1,
            //s = 1,
            //b = 1,
            l = 1l,
            d = 1.0,
            f = 1f,
            bool = true,
            //c = 'c',
            str = "str"
    )
    class A { }
}
