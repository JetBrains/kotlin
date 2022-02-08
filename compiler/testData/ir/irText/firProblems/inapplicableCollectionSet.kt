// WITH_STDLIB
// FULL_JDK

class Flaf(val javaName: String) {

    private val INSTANCES = mutableMapOf<String, Flaf>()

    fun forJavaName(javaName: String): Flaf {
        var result: Flaf? = INSTANCES[javaName]
        if (result == null) {
            result = INSTANCES["${javaName}_alternative"]
            if (result == null) {
                result = Flaf(javaName)
            }
            INSTANCES[javaName] = result
        }
        return result
    }

}
