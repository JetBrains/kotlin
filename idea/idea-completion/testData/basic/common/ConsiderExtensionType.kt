class A
open class B

class C : B()
interface D

fun <T> T.ext1() where T : B {}
fun B.ext2() {}
fun A.ext3() {}
fun <T> T.ext4() where T : B, T : D {}

fun usage() {
    val c = C()
    c.ext<caret>
}

// EXIST: ext1
// EXIST: ext2
// ABSENT: ext3
// ABSENT: ext4