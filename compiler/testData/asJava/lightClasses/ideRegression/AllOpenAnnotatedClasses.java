@test.AllOpen
public class C {
    private final int p;

    public C() { /* compiled code */ }

    public void f() { /* compiled code */ }

    public void g() { /* compiled code */ }

    public int getP() { /* compiled code */ }

    public static final class D {
        public D() { /* compiled code */ }

        public final void z() { /* compiled code */ }
    }

    @test.AllOpen
    public static class H {
        public H() { /* compiled code */ }

        public void j() { /* compiled code */ }
    }
}