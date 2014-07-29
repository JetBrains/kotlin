fun String.inc() = this + "+"

class Foo {
    {
        var s = ""
        <selection>s++</selection>
        s.inc()
        s = s.inc()
    }
}