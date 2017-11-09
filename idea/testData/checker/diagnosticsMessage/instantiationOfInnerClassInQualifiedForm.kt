class A {
    inner class XYZ

    fun foo() {
        val <warning>v</warning>: A.XYZ = A.<error descr="[RESOLUTION_TO_CLASSIFIER] Constructor of inner class XYZ can be called only with receiver of containing class">XYZ</error>()
    }
}