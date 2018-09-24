// FILE: ALambda.java

public interface ALambda {
    ALambda curried();

    ALambda tupled();
}

// FILE: ACheckedFunction0.java

public interface ACheckedFunction0 extends ALambda {

    Integer apply();

    default ALambda curried() { return null; }
    default ALambda tupled() { return null; }
}

// FILE: main.kt
fun test() {
    ACheckedFunction0 { 2 } // error: Interface ACheckedFunction0 does not have constructors
}
