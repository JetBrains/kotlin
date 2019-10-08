@Throws(IOException::class, ResponseParseException::class)
fun fetchPluginReleaseDate(pluginId: PluginId, version: String, channel: String?): LocalDate? {
    val url = "https://plugins.jetbrains.com/api/plugins/${pluginId.idString}/updates?version=$version"

    val pluginDTOs: Array<PluginDTO> = try {
        HttpRequests.request(url).connect {
            GsonBuilder().create().fromJson(it.inputStream.reader(), Array<PluginDTO>::class.java)
        }
    } catch (ioException: JsonIOException) {
        throw IOException(ioException)
    } catch (syntaxException: JsonSyntaxException) {
        throw ResponseParseException("Can't parse json response", syntaxException)
    }
}

interface AutoCloseable {
    fun close()
}

internal fun AutoCloseable?.closeFinally(cause: Throwable?) = when {
    this == null -> {}
    cause == null -> close()
    else ->
        try {
            close()
        } catch (closeException: Throwable) {
            cause.addSuppressed(closeException)
        }
}

inline fun <reified T : Any> Sequence<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}