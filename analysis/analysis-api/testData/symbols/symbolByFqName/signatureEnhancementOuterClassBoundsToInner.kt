// ISSUE: KT-70327
// class: /OuterClass.InnerClass
// FILE: OuterClass.java
public abstract class OuterClass<T extends OuterClass<T>.InnerClass> {
    public class InnerClass {

    }
}

// FILE: main.kt
