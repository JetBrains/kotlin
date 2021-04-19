// FILE: Calendar.java
public class Calendar {
    public void setTimeInMillis(long millis) {}
    public long getTimeInMillis() { return 1; }
}

// FILE: 1.kt
class A

var A.timeInMillis: String
    get() = ""
    set(v) {}

fun a(c: Calendar) {
    A().apply {
        c.apply {
            timeInMillis = 5 // synthesized variable for get|setTimeInMillis
            timeInMillis = <!ASSIGNMENT_TYPE_MISMATCH!>""<!>
        }
        timeInMillis = ""
    }
}
