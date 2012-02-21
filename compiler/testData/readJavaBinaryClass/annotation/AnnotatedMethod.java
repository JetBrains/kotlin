package test;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@interface Aaa {
}

class HasAnnotatedMethod {
    @Aaa
    public void f() { }
}
