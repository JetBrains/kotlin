public class NullableDeferredFunReturnType {
    public void main() {
        System.out.println(new A().foo().<warning descr="Method invocation 'length' may produce 'java.lang.NullPointerException'">length</warning>());
    }
}
