package common

import kotlinx.cinterop.CPointer
import platform.posix.FILE
import platform.posix.fopen
import platform.posix.fprintf
import platform.zlib.uInt

fun test() {
    val file: CPointer< /* NAVIGATION-TARGET:expect class FILE */ FILE> = /* NAVIGATION-TARGET:external expect fun fopen */ fopen("file.txt", "r") ?: return
    fun f1(): /* NAVIGATION-TARGET:expect class uInt */ uInt = TODO()
    /* NAVIGATION-TARGET:external expect fun fprintf */ fprintf(null, "")
}
