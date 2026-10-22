// DUMP_IR_DIFFERENCE: JKLIB
//   JKlib tests compile against a real JDK, whose default methods (`forEach`, `stream`, `spliterator`, ...) add fake overrides.
// FULL_JDK
// TARGET_BACKEND: JVM

class A1 : java.util.ArrayList<String>()

class A2 : java.util.ArrayList<String>() {
    override fun remove(x: String): Boolean = true
}
