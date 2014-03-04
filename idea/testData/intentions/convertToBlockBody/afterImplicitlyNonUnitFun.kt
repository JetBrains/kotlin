// "Convert to block body" "true"
import java.net.URI

fun foo(): URI {
    return java.io.File("x").toURI()
}