package common

import kotlinx.cinterop.*
import platform.posix.*

fun test() {
    val file: CPointer< /* NAVIGATION-TARGET:typealias FILE = */ FILE> = /* NAVIGATION-TARGET:external expect fun fopen */ fopen("file.txt", "r") ?: return
    /* NAVIGATION-TARGET:external expect fun fprintf */ fprintf(null, "")
    memScoped {
        val addr: CPointerVarOf<CPointer< /* NAVIGATION-TARGET:final expect class sockaddr_in */ sockaddr_in>> = allocPointerTo()
    }
}
