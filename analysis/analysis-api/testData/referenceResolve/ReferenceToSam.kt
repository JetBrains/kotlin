// FILE: main.kt
fun some() {
    val jClass = JavaTest.SomeJavaClass()
    jClass.<caret>setListener {}
}

// FILE: JavaTest.java
public class JavaTest {
    public interface SAMInterface {
        void onEvent(int event);
    }

    public static class SomeJavaClass {
        public void setListener(SAMInterface listener) {}
    }
}
