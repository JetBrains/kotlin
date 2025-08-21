// JVM_ABI_K1_K2_DIFF: K2 serializes annotation parameter default values (KT-59526).

annotation class A(val a: Int = 0)

@A fun test1() = 1
@A(2) fun test2() = 1

fun box(): String {
   if ((test1() + test2()) == 2) {
      return "OK"
   }
   return "fail"
}
