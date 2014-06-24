import java.lang.reflect.TypeVariable;

interface typeVariableRaw {
    interface Super<T extends Class> {
        TypeVariable<T> typeForSubstitute();
    }

    interface Mid extends Super {
    }
}