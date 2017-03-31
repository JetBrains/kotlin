// WITH_RUNTIME

data class Package(val name: String, val version: String, val source: String, val id: Int)

val pkgs = listOf<Package>().mapIndexed { i, <caret>p -> "${p.name} ${p.version}" to i + p.id }.toMap()