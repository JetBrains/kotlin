// IGNORE_BACKEND: JVM_IR
// FILE: JavaClass.java

public class JavaClass {

    public Double minus0(){
        return -0.0;
    }

    public Double plus0(){
        return 0.0;
    }

    public Double null0(){
        return null;
    }

}

// FILE: b.kt


fun box(): String {

    val jClass = JavaClass()

    if (jClass.minus0().compareTo(jClass.plus0()) != -1) return "fail 1"

    //TODO: KT-14989
    //if (jClass.null0().compareTo(jClass.plus0())) return "fail 2"
    try {
        if (jClass.minus0().compareTo(jClass.null0()) != -2) return "fail 3"
        return "fail: exception expected";
    } catch (e: IllegalStateException) {

    }

    return "OK"
}