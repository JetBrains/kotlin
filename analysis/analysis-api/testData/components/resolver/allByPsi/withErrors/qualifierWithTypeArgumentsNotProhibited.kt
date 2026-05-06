// LANGUAGE: -ForbidUselessTypeArgumentsIn25
// FILE: JavaClass.java

public class JavaClass<T> {
    public static Integer staticField = 1;
    public void method() {}
}

// FILE: main.kt

fun test() {
   JavaClass<Int>.staticField
   JavaClass<Long>.unresolved
   JavaClass<String>::staticField
   JavaClass<Boolean>::method
}
