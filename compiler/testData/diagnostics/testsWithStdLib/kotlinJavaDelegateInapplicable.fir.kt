// ISSUE: KT-57543
// RENDER_DIAGNOSTICS_MESSAGES
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

// FILE: Main.kt

class A(
    val c: Int? = 0,
    myType: (() -> Int)? = null
) {
    val arguments: A by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE("getValue(A, KProperty1<A, A>); [fun getValue(instance: Any!, metadata: Any!): ft<Stub (chain inference): TypeVariable(_T), Stub (chain inference): TypeVariable(_T)?>]"), NEW_INFERENCE_ERROR("NewConstraintError at Incorporate TypeVariable(_K) == kotlin/Nothing? from Fix variable TypeVariable(_K) from position Fix variable TypeVariable(_K): kotlin/Function0<TypeVariable(__R)> <!: kotlin/Nothing?")!>JavaClass.lazySoft {
        <!NEW_INFERENCE_ERROR("NewConstraintError at Incorporate TypeVariable(_K) == kotlin/Nothing? from Fix variable TypeVariable(_K) from position Fix variable TypeVariable(_K): kotlin/Function0<TypeVariable(__R)> <!: kotlin/Nothing?")!>A(myType = if (false) null else fun(): Int { return <!NEW_INFERENCE_ERROR("NewConstraintError at Incorporate TypeVariable(_K) == kotlin/Nothing? from Fix variable TypeVariable(_K) from position Fix variable TypeVariable(_K): kotlin/Function0<TypeVariable(__R)> <!: kotlin/Nothing?")!>c!!<!> })<!>
    }<!>
}
