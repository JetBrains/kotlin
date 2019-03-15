// FILE: JavaClass.java

public class JavaClass {

    public Double minus0(){
        return -0.0;
    }

    public Double plus0(){
        return 0.0;
    }

}


// FILE: b.kt

fun box(): String {
    val jClass = JavaClass()

    if (jClass.minus0().equals(jClass.plus0())) return "fail 1"
    if (jClass.plus0().equals(jClass.minus0())) return "fail 2"

    return "OK"
}

