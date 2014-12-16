import java.util.List;
import java.util.Set;

class X<A> {

}

interface Foo<A, B> {
    X<List<A>> foo(List<X<B>> a, X<Set<A>> b);
}

class SamTest {
    static <A, B> void test(Foo<A, B> foo) {

    }
}