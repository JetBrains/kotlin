// KT-5016 wrong StringBuilder append method invoked
class kt5016int {
    fun f1(num : Int) : String {
         return "Hello to all the $num!"
    }
}

// 0 INVOKEVIRTUAL java/lang/StringBuilder.append \(Ljava/lang/Object;\)Ljava/lang/StringBuilder
// 2 INVOKEVIRTUAL java/lang/StringBuilder.append \(Ljava/lang/String;\)Ljava/lang/StringBuilder
// 1 INVOKEVIRTUAL java/lang/StringBuilder.append \(I\)Ljava/lang/StringBuilder
// 1 INVOKEVIRTUAL java/lang/StringBuilder.toString
