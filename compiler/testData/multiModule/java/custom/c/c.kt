package test

import custom.*

public class KotlinC: AClass() {
    public fun returnA(): AClass {}

    public fun paramA(a: AClass) {}

    public fun paramB(b: BClass) {}

    public fun returnB(): BClass { }

    @AAnnotation(AEnum.AX) fun annoA() {}

    @BAnnotation fun annoB() {}
}