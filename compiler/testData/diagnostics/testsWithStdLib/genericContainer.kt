// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FIR_DUMP
// FILE: GenericContainer.java

public class GenericContainer<SELF extends GenericContainer<SELF>> {
    public GenericContainer(String dockerImageName) {

    }
}

// FILE: test.kt

val container = GenericContainer("nginx")

/* GENERATED_FIR_TAGS: capturedType, flexibleType, javaFunction, javaType, outProjection, propertyDeclaration,
starProjection, stringLiteral */
