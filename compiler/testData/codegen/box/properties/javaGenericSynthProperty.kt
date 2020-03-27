// TARGET_BACKEND: JVM

// FILE: Box.java

class Box<B> {
    public B f;
    public Box(B b) {
        this.f = b;
    }
}

// FILE: JOuter.java

public class JOuter<O1, O2> {

    public O1 o1;
    public O2 o2;

    public JOuter(O1 a1, O2 a2) {
        this.o1 = a1;
        this.o2 = a2;
    }

    public JInner<Box<O1>, ?> instance(O1 s1, O2 s2) {
        return new JInner<Box<O1>, O2>(new Box(s1), s2);
    }

    public JStatic<?, Box<O2>> staticInstance() {
        return new JStatic<O1, Box<O2>>(o1, new Box(o2));
    }

    public class JInner<I1, I2> {

        public I1 i1;
        public I2 i2;

        public JInner(I1 a1, I2 a2) {
            this.i1 = a1;
            this.i2 = a2;
        }

        public String getFoo() {
            String s1 = (String)o1;
            String s2 = ((Box<String>)i1).f;
            return s1 + s2;
        }

        public String getBar() {
            String s1 = (String)o2;
            String s2 = (String)i2;
            return s1 + s2;
        }
    }

    public static class JStatic<S1, S2> {
        public S1 ss1;
        public S2 ss2;

        public JStatic(S1 a1, S2 a2) {
            this.ss1 = a1;
            this.ss2 = a2;
        }

        public String getQux() {
            String s1 = (String)ss1;
            String s2 = ((Box<String>)ss2).f;
            return s1 + s2;
        }
    }
}

// FILE: kotlin.kt

fun box(): String {

    val o = JOuter<String, String>("1", "2")

    val r1 = o.instance("3", "4").foo
    if (r1 != "13") return "FAIL1: $r1"

    val r2 = o.instance("5", "6").bar
    if (r2 != "26") return "FAIL2: $r2"

    val r3 = o.staticInstance().qux
    if (r3 != "12") return "FAIL3: $r3"

    return "OK"
}
