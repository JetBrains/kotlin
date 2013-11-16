// FILE: Base.java

class Base {
    public static final String FOO = "initial";
}

// FILE: Derived.java

class Derived extends Base {

}

// FILE: test.kt

annotation class Ann(val s: String)

Ann(Derived.FOO) fun test() {}