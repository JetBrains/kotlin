// IGNORE_BACKEND: JVM_IR
// TODO KT-36638 Use 'java/lang/StringBuilder.append (C)Ljava/lang/StringBuilder;' when appending single character in JVM_IR

// KT-5016 wrong StringBuilder append method invoked
class kt5016 {
    fun f1(name : String) : String {
         return "Hello $name!"
    }
}

// 0 INVOKEVIRTUAL java/lang/StringBuilder.append \(Ljava/lang/Object;\)Ljava/lang/StringBuilder
// 2 INVOKEVIRTUAL java/lang/StringBuilder.append \(Ljava/lang/String;\)Ljava/lang/StringBuilder
// 1 INVOKEVIRTUAL java/lang/StringBuilder.toString
