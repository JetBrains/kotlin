// ISSUE: KT-18055

fun main() {
    data class Stat(val link: String? = null)

    var stat = Stat()

    if (stat.link != null) {
        takeString(stat.link)
    }
}

fun takeString(link: String) {}
