package test;

public interface InheritedAdapterAndDeclaration {
    public interface Super {
        void foo(Runnable r);
        void foo(jet.Function0<jet.Unit> r);
    }

    public interface Sub extends Super {
    }
}
