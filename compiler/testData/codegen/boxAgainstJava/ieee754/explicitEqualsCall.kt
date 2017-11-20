// LANGUAGE_VERSION: 1.1
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

    if (jClass.minus0().equals(jClass.plus0())) return "fail 5"

    if (jClass.null0().equals(jClass.plus0())) return "fail 6"
    if (jClass.minus0().equals(jClass.null0())) return "fail 7"
    return "OK"
}

