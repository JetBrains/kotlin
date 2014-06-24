import java.lang.reflect.TypeVariable;

interface typeVariableClass {
    interface Super<T> {
        TypeVariable<? super T> typeForSubstitute();
    }

    interface Mid<E> extends Super<E> {
    }
}