// FILE: Anno.java
public @interface Anno {
    String value();
}

// FILE: main.kt
@Anno(<expr>"13"</expr>)
class Foo
