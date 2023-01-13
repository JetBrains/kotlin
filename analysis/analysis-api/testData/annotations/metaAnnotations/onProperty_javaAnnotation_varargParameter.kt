// FILE: JavaAnno.java

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

@Target({FIELD, METHOD})
public @interface JavaAnno() {
    ElementType[] value();
}

// FILE: test.kt

import java.lang.annotation.ElementType.FIELD
import java.lang.annotation.ElementType.METHOD

class Test {
    @JavaAnno(FIELD, METHOD)
    val f<caret>oo = ""
}
