package dependency

interface X
interface Y : X
interface Z

operator fun X.contains(s: String): Boolean = true
fun Z.contains(s: String): Boolean = true
