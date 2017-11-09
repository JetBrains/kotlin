public final class Annotations {
    @p.R(s = "a")
    @p.R(s = "b")
    @p.R(s = "c")
    public final void repeatables1() { /* compiled code */ }

    @p.R(s = "a")
    public final void repeatables2() { /* compiled code */ }

    @p.R(s = "a")
    @p.S(g = "b")
    @p.R(s = "c")
    @p.S(g = "D")
    @p.R(s = "f")
    public final void repeatables3() { /* compiled code */ }

    public Annotations() { /* compiled code */ }
}
