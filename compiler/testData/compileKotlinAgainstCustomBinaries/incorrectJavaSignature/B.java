package test;
import java.util.List;
public class B extends A {
    public <T> T foo(List<T> t) { return null; }
    public boolean foo(List<Boolean> t) { return false; }
}
