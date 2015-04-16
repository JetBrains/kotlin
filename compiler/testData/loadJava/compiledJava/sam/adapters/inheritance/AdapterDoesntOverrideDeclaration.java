package test;

public interface AdapterDoesntOverrideDeclaration {
    public interface Super {
        void foo(kotlin.jvm.functions.Function0<kotlin.Unit> r);
    }

    public interface Sub extends Super {
        void foo(Runnable r);
    }
}
