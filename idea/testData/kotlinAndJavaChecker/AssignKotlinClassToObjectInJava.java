// KT-4355 IDEA complains when assigning Kotlin objects where java.lang.Object is expected
class AssignKotlinClassToObjectInJava {
    void test(KotlinTrait trait) {
        Object kotlinClass = new KotlinClass();
        Object kotlinTrait = trait;

        KotlinClass foo = null;
        foo.equals(foo);
    }
}
