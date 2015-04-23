package a;

import kotlin.jvm.functions.*;
import kotlin.*;

public class A {
    public class <caret>X {
        public X(Function0<String> f) {
            System.out.println(f.invoke());
        }
    }
}
