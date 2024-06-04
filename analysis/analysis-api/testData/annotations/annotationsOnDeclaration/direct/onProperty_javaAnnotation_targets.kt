// COMPILATION_ERRORS

// FILE: JavaAnno.java

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

@Target({FIELD, METHOD})
public @interface JavaAnno() {
    ElementType[] types();
}

// FILE: test.kt

import java.lang.annotation.ElementType.FIELD

class Test {
    @JavaAnno(FIELD)
    val f<caret>oo = ""
}