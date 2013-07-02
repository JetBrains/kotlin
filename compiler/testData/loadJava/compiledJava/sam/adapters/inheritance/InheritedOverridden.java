package test;

public interface InheritedOverridden {
    public class Super {
        public void foo(Runnable r) {
        }
    }

    public class Sub extends Super {
        public void foo(Runnable r) {
        }
    }
}
