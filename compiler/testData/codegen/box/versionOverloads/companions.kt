// LANGUAGE: +CompanionBlocks +CompanionExtensions
@file:OptIn(ExperimentalVersionOverloading::class)

class WithVersionedConstructor(
    val prefix: String,
    @IntroducedAt("1") val suffix: String = "Suffix",
) {
    companion {
        fun companionBlockFun(number: Int, @IntroducedAt("1") extraSuffix: String = "Block"): String =
            "CompanionBlock" + number + extraSuffix
    }
}

class WithClassicCompanion {
    companion object {
        fun classicCompanionFun(
            number: Int,
            @IntroducedAt("1") extraSuffix: String = "Classic",
        ): String = "ClassicCompanion" + number + extraSuffix
    }
}

class WithNamedClassicCompanion {
    companion object Named {
        fun String.namedCompanionExtension(
            @IntroducedAt("1") suffix: String = "Named",
        ): String = this + suffix
    }
}

companion fun WithVersionedConstructor.companionExtensionFun(
    number: Int,
    @IntroducedAt("1") extraSuffix: String = "Extension",
): String = "CompanionExtension" + number + extraSuffix

@Suppress("DEPRECATION")
fun box(): String {
    if (WithVersionedConstructor.companionBlockFun(3) != "CompanionBlock3Block") return "fail6"
    if (WithVersionedConstructor.companionExtensionFun(3) != "CompanionExtension3Extension") return "fail7"
    if (WithClassicCompanion.classicCompanionFun(3) != "ClassicCompanion3Classic") return "fail8"
    with(WithNamedClassicCompanion.Named) {
        if ("Companion".namedCompanionExtension() != "CompanionNamed") return "fail9"
    }

    return "OK"
}
