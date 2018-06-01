public class DeferredValReturnType {
    public void main() {
        A a = new A();
        System.out.println(a.getFoo().length());
        System.out.println(a.<error descr="'foo' has private access in 'A'">foo</error>.length());
    }
}
