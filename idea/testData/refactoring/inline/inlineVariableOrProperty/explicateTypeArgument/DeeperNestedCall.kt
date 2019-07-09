// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

import java.util.ArrayList

fun f() {
    val v : List<Int> = ArrayList(listOf())
    ArrayList(<caret>v)
}
