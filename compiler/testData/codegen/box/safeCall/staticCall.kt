fun box(): String = if (Thread?.currentThread() != null) "OK" else "Fail"
