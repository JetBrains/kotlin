@test.AllOpen
public class C {
    private final int p;

    public void f() { /* compiled code */ }

    public void g() { /* compiled code */ }

    public int getP() { /* compiled code */ }

    public C() { /* compiled code */ }

    public static final class D {
        public final void z() { /* compiled code */ }

        public D() { /* compiled code */ }
    }

    @test.AllOpen
    public static class H {
        public void j() { /* compiled code */ }

        public H() { /* compiled code */ }
    }
}