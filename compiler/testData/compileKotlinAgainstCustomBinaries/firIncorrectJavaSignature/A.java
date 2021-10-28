package test;
import java.util.List;
public class A {
    public <T> T foo(List<T> t) { return null; }
    public boolean foo(List<Boolean> t) { return false; }
}
