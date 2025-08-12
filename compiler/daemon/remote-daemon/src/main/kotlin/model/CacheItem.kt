package model

import java.io.File

data class CacheItem(
    val fingerprint: String,
    val file: File
)