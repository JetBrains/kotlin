// ISSUE: KT-38031

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
    W.<!OVERLOAD_RESOLUTION_AMBIGUITY!>E<!>().<!UNRESOLVED_REFERENCE!>length<!> // ambiguity in both FIR / old FE
    W.E.<!UNRESOLVED_REFERENCE!>length<!> // resolved with error to the class W.E in FIR and old FE
    W.E.w // resolved to static field W.e.W in FE
}
