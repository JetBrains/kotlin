// FULL_JDK
// SKIP_KLIB_TEST

class A1 : java.util.ArrayList<String>()

class A2 : java.util.ArrayList<String>() {
    override fun remove(x: String): Boolean = true
}
