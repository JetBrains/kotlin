// TODO: Enable when JS backend supports Java class library
// TARGET_BACKEND: JVM
public class SomeClass() : java.lang.Object() {
}

public fun box():String {
    System.out?.println(SomeClass().getClass())
    return "OK"
}
