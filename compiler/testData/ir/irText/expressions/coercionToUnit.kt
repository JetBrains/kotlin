// +JDK

val test1: () -> Unit = { 42 }

fun test2(mc: MutableCollection<String>) {
    mc.add("")
}

fun test3() {
    System.out?.println("Hello,")
    System.out?.println("world!")
}