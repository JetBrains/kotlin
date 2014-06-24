import java.lang.reflect.TypeVariable;

interface rawWildcardInTypeVariableClass {
    interface Super<T> {
        TypeVariable<? super T> typeForSubstitute();
    }

    interface Sub extends Super {
    }
}