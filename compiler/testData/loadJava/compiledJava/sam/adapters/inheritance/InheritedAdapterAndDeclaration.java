package test;

public interface InheritedAdapterAndDeclaration {
    public interface Super {
        void foo(Runnable r);
        void foo(kotlin.jvm.functions.Function0<kotlin.Unit> r);
    }

    public interface Sub extends Super {
    }
}
