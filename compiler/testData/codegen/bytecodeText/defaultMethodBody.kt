// IGNORE_BACKEND: JVM_IR
fun test(a: Int, z: String = try{"1"} catch (e: Exception) {"2"}) {
    "Default body"
}

//1 ILOAD 0\s*ALOAD 1\s*INVOKESTATIC DefaultMethodBodyKt\.test \(ILjava/lang/String;\)V
