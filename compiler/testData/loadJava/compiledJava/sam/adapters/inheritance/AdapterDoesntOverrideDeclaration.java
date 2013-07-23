package test;

public interface AdapterDoesntOverrideDeclaration {
    public interface Super {
        void foo(jet.Function0<jet.Unit> r);
    }

    public interface Sub extends Super {
        void foo(Runnable r);
    }
}
