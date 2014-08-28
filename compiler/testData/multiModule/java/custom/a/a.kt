package test

import custom.*

public class KotlinA: AClass() {
    fun returnA(): AClass {}

    fun paramA(p: AClass) {}

    AAnnotation fun annoA() {}
}