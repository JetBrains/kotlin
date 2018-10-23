import java.io.InputStream

fun InputStream.test() {
    readBytes()

    <!DEPRECATION!>readBytes<!>(1)
}
