// PlatformTypes

import java.util.Collections

class PlatformTypes {
    fun simplyPlatform() = Collections.singletonList("")[0]
    fun bothNullable() = Collections.emptyList<String>() ?: null
    fun bothNotNull() = Collections.emptyList<String>()!!
}