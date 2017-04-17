package test

open class KotlinClass : KotlinInterface.KotlinInner2() {

    inner class KotlinInner

}

interface KotlinInterface {
    open class KotlinInner2 : JavaClass2() {
        class KotlinInner3
    }
}

private fun getKotlinInner() = UseKotlinInner().kotlinInner

private fun getJavaInner() = UseKotlinInner().javaInner

private fun getKotlinInner3() = UseKotlinInner().kotlinInner3
