package test;

public interface NullInAnnotation {
    @interface Ann {
        String a();
        String[] b();
    }

    @Ann(a = null, b = {null})
    void foo();
}