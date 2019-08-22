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

//    val selectedPluginDTO = pluginDTOs
//        .firstOrNull {
//            it.listed && it.approve && (it.channel == channel || (it.channel == "" && channel == null))
//        }
//        ?: return null
//
//    val dateString = selectedPluginDTO.cdate ?: throw ResponseParseException("Empty cdate")
//
//    return try {
//        val dateLong = dateString.toLong()
//        Instant.ofEpochMilli(dateLong).atZone(ZoneOffset.UTC).toLocalDate()
//    } catch (e: NumberFormatException) {
//        throw ResponseParseException("Can't parse long date", e)
//    } catch (e: DateTimeException) {
//        throw ResponseParseException("Can't convert to date", e)
//    }
}