import kotlinx.cinterop.*
import sockets.*

val AF_INET: Byte = 2
val SOCK_STREAM = 1

fun main(args: Array<String>) {
    if (args.size == 0) {
        return
    }

    val port = atoi(args[0]).toShort()

    memScoped {

        val bufferLength = 100L
        val buffer = allocArray<CInt8Var>(bufferLength)
        val serverAddr = alloc<sockaddr_in>()

        val listenFd = socket(AF_INET.toInt(), SOCK_STREAM, 0)
                .ensureUnixCallResult { it >= 0 }

        with(serverAddr) {
            memset(this.ptr, 0, sockaddr_in.size)
            sin_family.value = AF_INET
            sin_addr.s_addr.value = htons(0).toInt()
            sin_port.value = htons(port)
        }

        bind(listenFd, serverAddr.ptr.reinterpret(), sockaddr_in.size.toInt())
                .ensureUnixCallResult { it == 0 }

        listen(listenFd, 10)
                .ensureUnixCallResult { it == 0 }

        val commFd = accept(listenFd, null, null)
                .ensureUnixCallResult { it >= 0 }

        while (true) {
            val length = read(commFd, buffer.ptr, bufferLength)
                    .ensureUnixCallResult { it >= 0 }

            if (length == 0L) {
                break
            }

            write(commFd, buffer.ptr, length)
                    .ensureUnixCallResult { it >= 0 }
        }
    }
}

// Not available through interop because declared as macro:
fun htons(value: Short) = ((value.toInt() ushr 8) or (value.toInt() shl 8)).toShort()

fun throwUnixError(): Nothing {
    perror(null) // TODO: store error message to exception instead.
    throw Error("UNIX call failed")
}

inline fun Int.ensureUnixCallResult(predicate: (Int) -> Boolean): Int {
    if (!predicate(this)) {
        throwUnixError()
    }
    return this
}

inline fun Long.ensureUnixCallResult(predicate: (Long) -> Boolean): Long {
    if (!predicate(this)) {
        throwUnixError()
    }
    return this
}