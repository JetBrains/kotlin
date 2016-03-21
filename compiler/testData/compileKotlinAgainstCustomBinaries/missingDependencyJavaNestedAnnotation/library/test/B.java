package test;

@A.Anno("B")
public interface B {
    @A.Anno("foo")
    <T> T foo(T t);
}
