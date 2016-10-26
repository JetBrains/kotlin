// TODO: Enable when JS backend supports Java class library
// IGNORE_BACKEND: JS
public class SomeClass() : java.lang.Object() {
}

public fun box():String {
    System.out?.println(SomeClass().getClass())
    return "OK"
}
