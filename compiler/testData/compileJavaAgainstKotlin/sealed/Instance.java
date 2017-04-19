package test;

public class Instance {
    // It is not possible to create Season instance (it's sealed in Kotlin)
    static Season create() {
        return new Season();
    }
}