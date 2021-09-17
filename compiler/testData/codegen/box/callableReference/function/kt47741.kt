
fun box(): String {
   val generateId = (1 .. Int.MAX_VALUE).iterator()::next
   return if (generateId() == 1) "OK" else "FAIL"
}
