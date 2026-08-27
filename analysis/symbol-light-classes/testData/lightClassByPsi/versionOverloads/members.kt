// LIBRARY_PLATFORMS: JVM

class Container {
    internal fun internalFunction(
        a: Int = 1,
        @IntroducedAt("1") b: String = "b",
    ) = "$a/$b"

    companion object {
        @JvmStatic
        fun staticFunction(
            a: Int = 1,
            @IntroducedAt("1") b: String = "b",
        ) = "$a/$b"
    }
}

object Singleton {
    fun member(
        a: Int = 1,
        @IntroducedAt("1") b: String = "b",
    ) = "$a/$b"
}

@Deprecated("Use the other one")
fun alreadyDeprecated(
    a: Int = 1,
    @IntroducedAt("1") b: String = "b",
) = "$a/$b"

fun <T> generic(
    a: T,
    @IntroducedAt("1") b: List<T> = emptyList(),
) = "$a/$b"

fun withVararg(
    vararg a: String,
    @IntroducedAt("1") b: Int = 1,
) = "${a.size}/$b"

// LIGHT_ELEMENTS_NO_DECLARATION: Container.class[internalFunction$main;staticFunction;staticFunction], MembersKt.class[alreadyDeprecated;generic;withVararg], Singleton.class[member]
