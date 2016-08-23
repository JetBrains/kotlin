import java.util.List;
import pack.A;

class JavaClass {
    public List<A> getA() {
        A a = new A(1, "", "");
        return Collections.singletonList(a);
    }

    public void takeA(A a){}
}