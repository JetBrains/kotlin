// IGNORE_BACKEND: JVM_IR
// KT-5016 wrong StringBuilder append method invoked
class kt5016intOrNull {
    fun f1(num : Int?) : String {
         return "Hello to all the $num!"
    }
}

// 1 INVOKEVIRTUAL java/lang/StringBuilder.append \(Ljava/lang/Object;\)Ljava/lang/StringBuilder
// 1 INVOKEVIRTUAL java/lang/StringBuilder.append \(Ljava/lang/String;\)Ljava/lang/StringBuilder
// 1 INVOKEVIRTUAL java/lang/StringBuilder.toString
