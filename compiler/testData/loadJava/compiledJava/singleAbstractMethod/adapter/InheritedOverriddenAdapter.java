package test;

public interface InheritedOverriddenAdapter {
    public class Super {
        public void foo(Runnable r) {
        }
    }

    public class Sub extends Super {
        public void foo(jet.Function0<jet.Unit> r) {
        }
    }
}
