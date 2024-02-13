// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ Different type of s - KT-65603

class A {
    private abstract class B {
        val s = object {}
    }

    private class C : B()
}