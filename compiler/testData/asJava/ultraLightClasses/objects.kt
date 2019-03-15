
class C {
    companion object {
        @JvmStatic fun foo() {}
        fun bar() {}
        @JvmStatic var x: String = ""

        var I.c: String
            @JvmStatic get() = "OK"
            @JvmStatic set(t: String) {}

        var c1: String
            get() = "OK"
            @JvmStatic set(t: String) {}
    }
    companion object Factory {}
}

class C1 {
  private companion object {}
}

interface I {
  companion object { }
}

object Obj : java.lang.Runnable {
    @JvmStatic var x: String = ""
    override fun run() {}
    @JvmStatic fun zoo(): Int = 2
}
