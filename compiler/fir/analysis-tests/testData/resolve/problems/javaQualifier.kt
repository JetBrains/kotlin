// FILE: W.java
public class W {
    public static class E {
        public static String w = "";
    }

    public static String E() {return "";}

    public static final String E = "";
}

// FILE: main.kt
fun main() {
    W.E().length // ambiguity in old FE, resolved to static method in FIR
    W.E.<!UNRESOLVED_REFERENCE!>length<!> // resolved with error to the class W.E in FIR and old FE
    W.E.w // resolved to static field W.e.W in FE
}
