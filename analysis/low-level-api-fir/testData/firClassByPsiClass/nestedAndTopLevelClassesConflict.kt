// FILE: usesite.kt
val a = 1

// FILE: pckg/Outer.java
class Outer {
    class AA {
    }
}

class Outer {
    class AA {
    }
    class A<caret>A {
        void correct(){}
    }
}
