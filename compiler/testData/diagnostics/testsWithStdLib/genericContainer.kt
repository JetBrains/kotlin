// FIR_IDENTICAL
// FIR_DUMP
// FILE: GenericContainer.java

public class GenericContainer<SELF extends GenericContainer<SELF>> {
    public GenericContainer(String dockerImageName) {

    }
}

// FILE: test.kt

val container = GenericContainer("nginx")
