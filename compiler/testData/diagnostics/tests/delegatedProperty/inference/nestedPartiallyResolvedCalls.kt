// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-57543

// FILE: JavaClass.java
import kotlin.jvm.functions.Function0;

public class JavaClass {

    public static class Val<T> {
        private final Function0<T> initializer;
        public Val(Function0<T> initializer) {
            this.initializer = initializer;
        }
        public final T getValue(Object instance, Object metadata) {
            return initializer.invoke();
        }
    }

    public static <T> Val<T> lazySoft(Function0<T> initializer) {
        return new Val<T>(initializer);
    }
}

// FILE: main.kt

class A(
    val c: Int? = 0,
    myType: (() -> Int)? = null
) {
    val arguments: A by JavaClass.lazySoft {
        A(myType = if (false) null else fun(): Int { return c!! })
    }
}
